package com.dongbacsaigon.backend.catalog.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.approval.service.ApprovalService;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.dto.AdminProductListItemResponse;
import com.dongbacsaigon.backend.catalog.dto.AdminProductPageResponse;
import com.dongbacsaigon.backend.catalog.dto.AdminProductResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductDraftRequest;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionSummaryResponse;
import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionImage;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionRelatedProduct;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRelatedProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductRevisionRepository revisionRepository;
    private final ProductRevisionImageRepository imageRepository;
    private final ProductRevisionRelatedProductRepository relatedRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final CatalogValidationService validationService;
    private final ApprovalService approvalService;
    private final AuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            ProductRevisionRepository revisionRepository,
            ProductRevisionImageRepository imageRepository,
            ProductRevisionRelatedProductRepository relatedRepository,
            ApprovalRequestRepository approvalRequestRepository,
            UserRepository userRepository,
            CatalogValidationService validationService,
            ApprovalService approvalService,
            AuditService auditService
    ) {
        this.productRepository = productRepository;
        this.revisionRepository = revisionRepository;
        this.imageRepository = imageRepository;
        this.relatedRepository = relatedRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.approvalService = approvalService;
        this.auditService = auditService;
    }

    @Transactional
    public ProductRevisionResponse create(ProductDraftRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        Product product = productRepository.save(new Product(actor));
        CatalogValidationService.ResolvedDraft draft = validationService.resolveDraft(request, product.getId());
        ProductRevision revision = new ProductRevision(
                product,
                product.nextRevisionNumber(),
                draft.category(),
                draft.name(),
                draft.slug(),
                draft.shortDescription(),
                draft.content(),
                draft.seoTitle(),
                draft.seoDescription(),
                actor
        );
        revisionRepository.save(revision);
        saveRelationships(revision, draft);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_CREATED, AuditTargetType.PRODUCT, product.getId(), "revision 1");
        return toRevisionResponse(revision);
    }

    @Transactional
    public ProductRevisionResponse updateDraft(UUID productId, UUID revisionId, ProductDraftRequest request, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        requireNotArchived(product);
        ProductRevision revision = requireRevision(productId, revisionId);
        requireDraft(revision);
        User actor = requireUser(actorId);
        CatalogValidationService.ResolvedDraft draft = validationService.resolveDraft(request, productId);
        revision.updateDraft(
                draft.category(),
                draft.name(),
                draft.slug(),
                draft.shortDescription(),
                draft.content(),
                draft.seoTitle(),
                draft.seoDescription()
        );
        imageRepository.deleteByRevisionId(revisionId);
        relatedRepository.deleteByRevisionId(revisionId);
        imageRepository.flush();
        relatedRepository.flush();
        saveRelationships(revision, draft);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_REVISION_UPDATED, AuditTargetType.PRODUCT_REVISION, revisionId, "revision " + revision.getRevisionNumber());
        return toRevisionResponse(revision);
    }

    @Transactional
    public ProductRevisionResponse createRevision(UUID productId, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        requireNotArchived(product);
        if (revisionRepository.existsByProductIdAndStatusIn(
                productId,
                List.of(ProductRevisionStatus.DRAFT, ProductRevisionStatus.PENDING_REVIEW)
        )) {
            throw new ApiException(HttpStatus.CONFLICT, "Product already has an active draft or pending revision.");
        }
        ProductRevision latest = revisionRepository.findFirstByProductIdOrderByRevisionNumberDesc(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Product has no revision to clone."));
        ProductRevision source = latest.getStatus() == ProductRevisionStatus.REJECTED
                ? latest
                : product.getCurrentPublishedRevision();
        if (source == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Product has no rejected or published revision to clone.");
        }
        User actor = requireUser(actorId);
        ProductRevision revision = new ProductRevision(
                product,
                product.nextRevisionNumber(),
                source.getCategory(),
                source.getName(),
                source.getSlug(),
                source.getShortDescription(),
                source.getContent(),
                source.getSeoTitle(),
                source.getSeoDescription(),
                actor
        );
        revisionRepository.save(revision);
        cloneRelationships(source, revision);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_REVISION_CREATED, AuditTargetType.PRODUCT_REVISION, revision.getId(), "revision " + revision.getRevisionNumber());
        return toRevisionResponse(revision);
    }

    @Transactional
    public ProductRevisionResponse submit(UUID productId, UUID revisionId, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        requireNotArchived(product);
        ProductRevision revision = requireRevision(productId, revisionId);
        requireDraft(revision);
        validationService.validateStoredRevision(revision);
        approvalService.submitForReview(
                ApprovalResourceType.PRODUCT,
                productId,
                revision.getRevisionNumber(),
                actorId
        );
        revision.submit();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_REVISION_SUBMITTED, AuditTargetType.PRODUCT_REVISION, revisionId, "revision " + revision.getRevisionNumber());
        return toRevisionResponse(revision);
    }

    @Transactional(readOnly = true)
    public AdminProductPageResponse list(
            int page,
            int size,
            String search,
            UUID categoryId,
            ProductPublicationStatus publicationStatus,
            ProductRevisionStatus revisionStatus
    ) {
        Page<ProductRevision> revisions = revisionRepository.findAdminPage(
                normalizeSearch(search),
                categoryId,
                publicationStatus,
                revisionStatus,
                PageRequest.of(validatePage(page), validateSize(size), Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        List<AdminProductListItemResponse> content = revisions.getContent().stream()
                .map(latest -> {
                    Product product = latest.getProduct();
                    return new AdminProductListItemResponse(
                            product.getId(),
                            product.getPublicationStatus(),
                            product.getLatestRevisionNumber(),
                            CatalogMapper.toRevisionSummary(latest),
                            CatalogMapper.toRevisionSummary(product.getCurrentPublishedRevision()),
                            product.getCreatedAt(),
                            product.getUpdatedAt()
                    );
                })
                .toList();
        return new AdminProductPageResponse(content, revisions.getNumber(), revisions.getSize(), revisions.getTotalElements(), revisions.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminProductResponse get(UUID productId) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
        List<ProductRevision> revisions = revisionRepository.findByProductIdOrderByRevisionNumberDesc(productId);
        ProductRevision latest = revisions.isEmpty() ? null : revisions.get(0);
        ProductRevision draft = revisions.stream().filter(item -> item.getStatus() == ProductRevisionStatus.DRAFT).findFirst().orElse(null);
        return new AdminProductResponse(
                product.getId(),
                product.getPublicationStatus(),
                product.getLatestRevisionNumber(),
                CatalogMapper.toRevisionSummary(product.getCurrentPublishedRevision()),
                CatalogMapper.toRevisionSummary(latest),
                CatalogMapper.toRevisionSummary(draft),
                CatalogMapper.toUserSummary(product.getCreatedBy()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductRevisionSummaryResponse> listRevisions(UUID productId) {
        requireProduct(productId);
        return revisionRepository.findByProductIdOrderByRevisionNumberDesc(productId).stream()
                .map(CatalogMapper::toRevisionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductRevisionResponse getRevision(UUID productId, UUID revisionId) {
        return toRevisionResponse(requireRevision(productId, revisionId));
    }

    @Transactional
    public void deleteDraftProduct(UUID productId, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        List<ProductRevision> revisions = revisionRepository.findByProductIdOrderByRevisionNumberDesc(productId);
        boolean unsafe = product.getCurrentPublishedRevision() != null
                || product.getPublicationStatus() != ProductPublicationStatus.DRAFT
                || revisions.stream().anyMatch(revision -> revision.getStatus() != ProductRevisionStatus.DRAFT)
                || approvalRequestRepository.existsByResourceTypeAndResourceId(ApprovalResourceType.PRODUCT, productId)
                || relatedRepository.existsByRelatedProductId(productId);
        if (unsafe) {
            throw new ApiException(HttpStatus.CONFLICT, "Product cannot be hard deleted after submission, publication, or external reference.");
        }
        for (ProductRevision revision : revisions) {
            imageRepository.deleteByRevisionId(revision.getId());
            relatedRepository.deleteByRevisionId(revision.getId());
        }
        imageRepository.flush();
        relatedRepository.flush();
        revisionRepository.deleteAll(revisions);
        revisionRepository.flush();
        productRepository.delete(product);
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_DRAFT_DELETED, AuditTargetType.PRODUCT, productId, null);
    }

    @Transactional
    public AdminProductResponse unpublish(UUID productId, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        if (product.getPublicationStatus() != ProductPublicationStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a published product can be unpublished.");
        }
        product.unpublish();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_UNPUBLISHED, AuditTargetType.PRODUCT, productId, null);
        return get(productId);
    }

    @Transactional
    public AdminProductResponse republish(UUID productId, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        if (product.getPublicationStatus() != ProductPublicationStatus.UNPUBLISHED
                || product.getCurrentPublishedRevision() == null
                || product.getCurrentPublishedRevision().getStatus() != ProductRevisionStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an unpublished product with an approved current revision can be republished.");
        }
        validationService.validateStoredRevision(product.getCurrentPublishedRevision());
        product.republish();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_REPUBLISHED, AuditTargetType.PRODUCT, productId, null);
        return get(productId);
    }

    @Transactional
    public AdminProductResponse archive(UUID productId, UUID actorId) {
        Product product = requireProductForUpdate(productId);
        if (product.getPublicationStatus() == ProductPublicationStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "Product is already archived.");
        }
        product.archive();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.PRODUCT_ARCHIVED, AuditTargetType.PRODUCT, productId, null);
        return get(productId);
    }

    private void saveRelationships(ProductRevision revision, CatalogValidationService.ResolvedDraft draft) {
        imageRepository.saveAll(draft.images().stream()
                .map(image -> new ProductRevisionImage(revision, image.media(), image.sortOrder(), image.primary()))
                .toList());
        relatedRepository.saveAll(draft.relatedProducts().stream()
                .map(related -> new ProductRevisionRelatedProduct(revision, related.product(), related.sortOrder()))
                .toList());
    }

    private void cloneRelationships(ProductRevision source, ProductRevision target) {
        imageRepository.saveAll(imageRepository.findByRevisionIdOrderBySortOrderAsc(source.getId()).stream()
                .map(image -> new ProductRevisionImage(target, image.getMedia(), image.getSortOrder(), image.isPrimary()))
                .toList());
        relatedRepository.saveAll(relatedRepository.findByRevisionIdOrderBySortOrderAsc(source.getId()).stream()
                .map(related -> new ProductRevisionRelatedProduct(target, related.getRelatedProduct(), related.getSortOrder()))
                .toList());
    }

    private ProductRevisionResponse toRevisionResponse(ProductRevision revision) {
        return CatalogMapper.toRevisionResponse(
                revision,
                imageRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId()),
                relatedRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId())
        );
    }

    private Product requireProductForUpdate(UUID id) {
        return productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private ProductRevision requireRevision(UUID productId, UUID revisionId) {
        return revisionRepository.findByIdAndProductId(revisionId, productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product revision not found."));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private void requireDraft(ProductRevision revision) {
        if (revision.getStatus() != ProductRevisionStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a draft revision can be modified or submitted.");
        }
    }

    private void requireNotArchived(Product product) {
        if (product.getPublicationStatus() == ProductPublicationStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "Archived products cannot be changed.");
        }
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;
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
}
