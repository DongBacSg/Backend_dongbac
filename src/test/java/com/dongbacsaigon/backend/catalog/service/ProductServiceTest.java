package com.dongbacsaigon.backend.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.approval.service.ApprovalService;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.dto.ProductDraftRequest;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionResponse;
import com.dongbacsaigon.backend.catalog.entity.Category;
import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRelatedProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductRevisionRepository revisionRepository = mock(ProductRevisionRepository.class);
    private final ProductRevisionImageRepository imageRepository = mock(ProductRevisionImageRepository.class);
    private final ProductRevisionRelatedProductRepository relatedRepository = mock(ProductRevisionRelatedProductRepository.class);
    private final ApprovalRequestRepository approvalRequestRepository = mock(ApprovalRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CatalogValidationService validationService = mock(CatalogValidationService.class);
    private final ApprovalService approvalService = mock(ApprovalService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ProductService service = new ProductService(
            productRepository,
            revisionRepository,
            imageRepository,
            relatedRepository,
            approvalRequestRepository,
            userRepository,
            validationService,
            approvalService,
            auditService
    );

    @Test
    void productCreationCreatesServerNumberedRevisionOneDraft() {
        User staff = staff();
        Category category = category(staff);
        ProductDraftRequest request = request(category.getId());
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.resolveDraft(any(), any())).thenReturn(resolved(category));
        when(revisionRepository.save(any(ProductRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageRepository.findByRevisionIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(relatedRepository.findByRevisionIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        ProductRevisionResponse response = service.create(request, staff.getId());

        assertThat(response.revisionNumber()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(ProductRevisionStatus.DRAFT);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getPublicationStatus()).isEqualTo(ProductPublicationStatus.DRAFT);
        verify(auditService).recordSuccessAfterCommit(
                staff,
                AuditAction.PRODUCT_CREATED,
                AuditTargetType.PRODUCT,
                productCaptor.getValue().getId(),
                "revision 1"
        );
    }

    @Test
    void submittedRevisionBecomesPendingAndUsesStableApprovalContract() {
        User staff = staff();
        Category category = category(staff);
        Product product = new Product(staff);
        ProductRevision revision = revision(product, category, staff);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
        when(revisionRepository.findByIdAndProductId(revision.getId(), product.getId())).thenReturn(Optional.of(revision));
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(imageRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId())).thenReturn(List.of());
        when(relatedRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId())).thenReturn(List.of());

        ProductRevisionResponse response = service.submit(product.getId(), revision.getId(), staff.getId());

        assertThat(response.status()).isEqualTo(ProductRevisionStatus.PENDING_REVIEW);
        verify(approvalService).submitForReview(
                ApprovalResourceType.PRODUCT,
                product.getId(),
                1,
                staff.getId()
        );
    }

    @Test
    void failedApprovalSubmissionLeavesRevisionAsDraft() {
        User staff = staff();
        Category category = category(staff);
        Product product = new Product(staff);
        ProductRevision revision = revision(product, category, staff);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
        when(revisionRepository.findByIdAndProductId(revision.getId(), product.getId())).thenReturn(Optional.of(revision));
        org.mockito.Mockito.doThrow(new IllegalStateException("approval failed"))
                .when(approvalService)
                .submitForReview(ApprovalResourceType.PRODUCT, product.getId(), 1, staff.getId());

        assertThatThrownBy(() -> service.submit(product.getId(), revision.getId(), staff.getId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(revision.getStatus()).isEqualTo(ProductRevisionStatus.DRAFT);
    }

    @Test
    void pendingRevisionIsImmutable() {
        User staff = staff();
        Category category = category(staff);
        Product product = new Product(staff);
        ProductRevision revision = revision(product, category, staff);
        revision.submit();
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
        when(revisionRepository.findByIdAndProductId(revision.getId(), product.getId())).thenReturn(Optional.of(revision));

        assertThatThrownBy(() -> service.updateDraft(product.getId(), revision.getId(), request(category.getId()), staff.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Only a draft revision can be modified or submitted.");
    }

    @Test
    void newRevisionClonesPublishedRevisionAndIncrementsNumber() {
        User staff = staff();
        User admin = admin();
        Category category = category(admin);
        Product product = new Product(staff);
        ProductRevision published = revision(product, category, staff);
        published.publish(admin, java.time.Instant.now());
        product.publish(published);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
        when(revisionRepository.existsByProductIdAndStatusIn(any(), any())).thenReturn(false);
        when(revisionRepository.findFirstByProductIdOrderByRevisionNumberDesc(product.getId())).thenReturn(Optional.of(published));
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(revisionRepository.save(any(ProductRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageRepository.findByRevisionIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(relatedRepository.findByRevisionIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        ProductRevisionResponse response = service.createRevision(product.getId(), staff.getId());

        assertThat(response.revisionNumber()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ProductRevisionStatus.DRAFT);
        assertThat(response.name()).isEqualTo(published.getName());
    }

    @Test
    void adminCanUnpublishRepublishAndArchiveWithoutPublishingNewDraft() {
        User admin = admin();
        Category category = category(admin);
        Product product = new Product(admin);
        ProductRevision published = revision(product, category, admin);
        published.publish(admin, java.time.Instant.now());
        product.publish(published);
        when(productRepository.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.findDetailedById(product.getId())).thenReturn(Optional.of(product));
        when(revisionRepository.findByProductIdOrderByRevisionNumberDesc(product.getId())).thenReturn(List.of(published));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        service.unpublish(product.getId(), admin.getId());
        assertThat(product.getPublicationStatus()).isEqualTo(ProductPublicationStatus.UNPUBLISHED);

        service.republish(product.getId(), admin.getId());
        assertThat(product.getPublicationStatus()).isEqualTo(ProductPublicationStatus.PUBLISHED);
        assertThat(product.getCurrentPublishedRevision()).isSameAs(published);

        service.archive(product.getId(), admin.getId());
        assertThat(product.getPublicationStatus()).isEqualTo(ProductPublicationStatus.ARCHIVED);
        assertThat(published.getStatus()).isEqualTo(ProductRevisionStatus.PUBLISHED);
    }

    private ProductRevision revision(Product product, Category category, User user) {
        return new ProductRevision(
                product,
                product.nextRevisionNumber(),
                category,
                "Product",
                "product",
                "Short",
                "Content",
                null,
                null,
                user
        );
    }

    private CatalogValidationService.ResolvedDraft resolved(Category category) {
        return new CatalogValidationService.ResolvedDraft(
                category,
                "Product",
                "product",
                "Short",
                "Content",
                null,
                null,
                List.of(),
                List.of()
        );
    }

    private ProductDraftRequest request(UUID categoryId) {
        return new ProductDraftRequest(categoryId, "Product", null, "Short", "Content", null, null, List.of(), List.of());
    }

    private Category category(User user) {
        return new Category("Category", "category", null, 0, true, user);
    }

    private User admin() {
        return User.admin("admin@example.com", "$2a$12$hash", "Admin");
    }

    private User staff() {
        return User.staff("staff@example.com", "$2a$12$hash", "Staff", UUID.randomUUID());
    }
}
