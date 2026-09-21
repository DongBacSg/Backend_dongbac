package com.dongbacsaigon.backend.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.dto.CategoryCreateRequest;
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
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductRevisionRepository revisionRepository = mock(ProductRevisionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final CategoryService service = new CategoryService(
            categoryRepository,
            productRepository,
            revisionRepository,
            userRepository,
            new SlugService(),
            auditService
    );

    @Test
    void adminCreatesCategoryWithGeneratedSlug() {
        User admin = admin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.create(
                new CategoryCreateRequest(" Phân bón lá ", null, null, null, null),
                admin.getId()
        );

        assertThat(response.name()).isEqualTo("Phân bón lá");
        assertThat(response.slug()).isEqualTo("phan-bon-la");
        assertThat(response.active()).isTrue();
        verify(auditService).recordSuccessAfterCommit(
                admin,
                AuditAction.CATEGORY_CREATED,
                AuditTargetType.CATEGORY,
                response.id(),
                null
        );
    }

    @Test
    void duplicateCategorySlugIsRejected() {
        User admin = admin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(categoryRepository.existsBySlug("phan-bon-la")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CategoryCreateRequest("Phân bón lá", null, null, 0, true),
                admin.getId()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category slug already exists.");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void categoryWithPublishedProductsCannotBeDeactivated() {
        User admin = admin();
        Category category = new Category("Category", "category", null, 0, true, admin);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(productRepository.existsPublicProductByCategory(category.getId(), ProductPublicationStatus.PUBLISHED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(
                category.getId(),
                new CategoryUpdateRequest(null, null, null, null, false),
                admin.getId()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category cannot be deactivated while it contains published products.");
    }

    @Test
    void categoryReferencedByRevisionCannotBeDeleted() {
        User admin = admin();
        Category category = new Category("Category", "category", null, 0, true, admin);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(revisionRepository.existsByCategoryId(category.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(category.getId(), admin.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category is referenced by product revisions.");
    }

    @Test
    void publicListReadsOnlyActiveCategories() {
        User admin = admin();
        Category category = new Category("Active", "active", null, 0, true, admin);
        when(categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(category));

        assertThat(service.listPublic()).extracting(CategoryResponse::slug).containsExactly("active");
        verify(categoryRepository).findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Test
    void inactiveCategoryDoesNotResolvePublicly() {
        when(categoryRepository.findBySlugAndActiveTrue("inactive")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublic("inactive"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category not found.");
    }

    private User admin() {
        return User.admin("admin@example.com", "$2a$12$hash", "Admin");
    }
}
