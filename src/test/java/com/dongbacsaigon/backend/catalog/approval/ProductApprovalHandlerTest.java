package com.dongbacsaigon.backend.catalog.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.entity.Category;
import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.catalog.service.CatalogValidationService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;

class ProductApprovalHandlerTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductRevisionRepository revisionRepository = mock(ProductRevisionRepository.class);
    private final CatalogValidationService validationService = mock(CatalogValidationService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ProductApprovalHandler handler = new ProductApprovalHandler(
            productRepository,
            revisionRepository,
            validationService,
            auditService
    );

    @Test
    void approvingFirstRevisionPublishesProductAndRevision() {
        Fixture fixture = pendingFixture();
        stubLocked(fixture);

        handler.onApproved(fixture.approval(), fixture.admin());

        assertThat(fixture.product().getPublicationStatus()).isEqualTo(ProductPublicationStatus.PUBLISHED);
        assertThat(fixture.product().getCurrentPublishedRevision()).isSameAs(fixture.revision());
        assertThat(fixture.revision().getStatus()).isEqualTo(ProductRevisionStatus.PUBLISHED);
        assertThat(fixture.revision().getPublishedBy()).isSameAs(fixture.admin());
        assertThat(fixture.revision().getPublishedAt()).isNotNull();
    }

    @Test
    void approvingNewRevisionArchivesPreviousPublishedRevision() {
        Fixture fixture = pendingFixture();
        ProductRevision previous = new ProductRevision(
                fixture.product(),
                2,
                fixture.revision().getCategory(),
                "Old",
                "old",
                null,
                null,
                null,
                null,
                fixture.admin()
        );
        previous.publish(fixture.admin(), Instant.now());
        fixture.product().publish(previous);
        stubLocked(fixture);

        handler.onApproved(fixture.approval(), fixture.admin());

        assertThat(previous.getStatus()).isEqualTo(ProductRevisionStatus.ARCHIVED);
        assertThat(fixture.product().getCurrentPublishedRevision()).isSameAs(fixture.revision());
        verify(revisionRepository).flush();
    }

    @Test
    void rejectionLeavesExistingPublicRevisionUnchanged() {
        Fixture fixture = pendingFixture();
        ProductRevision previous = new ProductRevision(
                fixture.product(),
                2,
                fixture.revision().getCategory(),
                "Old",
                "old",
                null,
                null,
                null,
                null,
                fixture.admin()
        );
        previous.publish(fixture.admin(), Instant.now());
        fixture.product().publish(previous);
        stubLocked(fixture);

        handler.onRejected(fixture.approval(), fixture.admin());

        assertThat(fixture.revision().getStatus()).isEqualTo(ProductRevisionStatus.REJECTED);
        assertThat(fixture.product().getCurrentPublishedRevision()).isSameAs(previous);
        assertThat(fixture.product().getPublicationStatus()).isEqualTo(ProductPublicationStatus.PUBLISHED);
    }

    @Test
    void failedRevalidationDoesNotPublishRevision() {
        Fixture fixture = pendingFixture();
        stubLocked(fixture);
        org.mockito.Mockito.doThrow(new ApiException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Product category is inactive."
        )).when(validationService).validateStoredRevision(fixture.revision());

        assertThatThrownBy(() -> handler.onApproved(fixture.approval(), fixture.admin()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Product category is inactive.");
        assertThat(fixture.revision().getStatus()).isEqualTo(ProductRevisionStatus.PENDING_REVIEW);
        assertThat(fixture.product().getPublicationStatus()).isEqualTo(ProductPublicationStatus.DRAFT);
    }

    private Fixture pendingFixture() {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", admin.getId());
        Category category = new Category("Category", "category", null, 0, true, admin);
        Product product = new Product(staff);
        ProductRevision revision = new ProductRevision(
                product,
                product.nextRevisionNumber(),
                category,
                "Product",
                "product",
                null,
                null,
                null,
                null,
                staff
        );
        revision.submit();
        ApprovalRequest approval = new ApprovalRequest(
                ApprovalResourceType.PRODUCT,
                product.getId(),
                revision.getRevisionNumber(),
                staff,
                Instant.now()
        );
        return new Fixture(admin, product, revision, approval);
    }

    private void stubLocked(Fixture fixture) {
        when(productRepository.findByIdForUpdate(fixture.product().getId())).thenReturn(Optional.of(fixture.product()));
        when(revisionRepository.findByProductAndNumberForUpdate(
                fixture.product().getId(),
                fixture.revision().getRevisionNumber()
        )).thenReturn(Optional.of(fixture.revision()));
    }

    private record Fixture(User admin, Product product, ProductRevision revision, ApprovalRequest approval) {
    }
}
