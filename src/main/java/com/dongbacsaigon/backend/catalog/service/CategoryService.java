package com.dongbacsaigon.backend.catalog.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.dto.CategoryCreateRequest;
import com.dongbacsaigon.backend.catalog.dto.CategoryPageResponse;
import com.dongbacsaigon.backend.catalog.dto.CategoryResponse;
import com.dongbacsaigon.backend.catalog.dto.CategoryUpdateRequest;
import com.dongbacsaigon.backend.catalog.entity.Category;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.repository.CategoryRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductRevisionRepository revisionRepository;
    private final UserRepository userRepository;
    private final SlugService slugService;
    private final AuditService auditService;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductRevisionRepository revisionRepository,
            UserRepository userRepository,
            SlugService slugService,
            AuditService auditService
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.revisionRepository = revisionRepository;
        this.userRepository = userRepository;
        this.slugService = slugService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CategoryPageResponse listAdmin(int page, int size, String search, Boolean active) {
        String normalizedSearch = normalizeSearch(search);
        Specification<Category> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            if (normalizedSearch != null) {
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), "%" + normalizedSearch + "%"),
                        builder.like(builder.lower(root.get("slug")), "%" + normalizedSearch + "%")
                ));
            }
            return predicate;
        };
        Page<CategoryResponse> result = categoryRepository.findAll(
                specification,
                PageRequest.of(validatePage(page), validateSize(size), Sort.by("sortOrder").ascending().and(Sort.by("name")))
        ).map(CatalogMapper::toCategoryResponse);
        return new CategoryPageResponse(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getAdmin(UUID id) {
        return CatalogMapper.toCategoryResponse(requireCategory(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listPublic() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(CatalogMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getPublic(String slug) {
        String normalizedSlug = slugService.normalize(slug);
        return categoryRepository.findBySlugAndActiveTrue(normalizedSlug)
                .map(CatalogMapper::toCategoryResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        String name = normalizeRequired(request.name(), "Category name is required.");
        String slug = slugService.normalize(StringUtils.hasText(request.slug()) ? request.slug() : name);
        requireUniqueSlug(slug, null);
        Category category = new Category(
                name,
                slug,
                normalizeOptional(request.description()),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.active() == null || request.active(),
                actor
        );
        categoryRepository.save(category);
        auditService.recordSuccessAfterCommit(actor, AuditAction.CATEGORY_CREATED, AuditTargetType.CATEGORY, category.getId(), null);
        return CatalogMapper.toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryUpdateRequest request, UUID actorId) {
        Category category = requireCategory(id);
        User actor = requireUser(actorId);
        String name = request.name() == null ? category.getName() : normalizeRequired(request.name(), "Category name is required.");
        String slug = request.slug() == null ? category.getSlug() : slugService.normalize(request.slug());
        boolean wasActive = category.isActive();
        boolean active = request.active() == null ? wasActive : request.active();
        if (wasActive && !active && productRepository.existsPublicProductByCategory(id, ProductPublicationStatus.PUBLISHED)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category cannot be deactivated while it contains published products.");
        }
        requireUniqueSlug(slug, id);
        category.update(
                name,
                slug,
                request.description() == null ? category.getDescription() : normalizeOptional(request.description()),
                request.sortOrder() == null ? category.getSortOrder() : request.sortOrder(),
                active,
                actor
        );
        AuditAction action = wasActive == active
                ? AuditAction.CATEGORY_UPDATED
                : (active ? AuditAction.CATEGORY_ACTIVATED : AuditAction.CATEGORY_DEACTIVATED);
        auditService.recordSuccessAfterCommit(actor, action, AuditTargetType.CATEGORY, category.getId(), null);
        return CatalogMapper.toCategoryResponse(category);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        Category category = requireCategory(id);
        if (revisionRepository.existsByCategoryId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category is referenced by product revisions.");
        }
        User actor = requireUser(actorId);
        categoryRepository.delete(category);
        auditService.recordSuccessAfterCommit(actor, AuditAction.CATEGORY_DELETED, AuditTargetType.CATEGORY, id, null);
    }

    public Category requireActiveCategory(UUID id) {
        Category category = requireCategory(id);
        if (!category.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category must be active.");
        }
        return category;
    }

    private Category requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private void requireUniqueSlug(String slug, UUID excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, excludedId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "Category slug already exists.");
        }
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0.");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size == 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100.");
        }
        return size;
    }

    private String normalizeSearch(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
