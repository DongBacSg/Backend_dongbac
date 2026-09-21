package com.dongbacsaigon.backend.catalog.approval;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.service.ApprovalResourceHandler;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.catalog.service.CatalogValidationService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ProductApprovalHandler implements ApprovalResourceHandler {

    private final ProductRepository productRepository;
    private final ProductRevisionRepository revisionRepository;
    private final CatalogValidationService validationService;
    private final AuditService auditService;

    public ProductApprovalHandler(
            ProductRepository productRepository,
            ProductRevisionRepository revisionRepository,
            CatalogValidationService validationService,
            AuditService auditService
    ) {
        this.productRepository = productRepository;
        this.revisionRepository = revisionRepository;
        this.validationService = validationService;
        this.auditService = auditService;
    }

    @Override
    public boolean supports(ApprovalResourceType resourceType) {
        return resourceType == ApprovalResourceType.PRODUCT;
    }

    @Override
    public void validateCanSubmit(
            ApprovalResourceType resourceType,
            UUID resourceId,
            long resourceVersion,
            UUID submittedBy
    ) {
        Product product = requireProduct(resourceId);
        requireNotArchived(product);
        ProductRevision revision = requireRevision(resourceId, resourceVersion);
        if (revision.getStatus() != ProductRevisionStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a draft product revision can be submitted.");
        }
        validationService.validateStoredRevision(revision);
    }

    @Override
    public void onApproved(ApprovalRequest approvalRequest) {
        User reviewer = approvalRequest.getReviewedBy();
        if (reviewer == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Product approval requires a reviewer.");
        }
        publish(approvalRequest, reviewer);
    }

    @Override
    public void onApproved(ApprovalRequest approvalRequest, User reviewer) {
        publish(approvalRequest, reviewer);
    }

    @Override
    public void onRejected(ApprovalRequest approvalRequest) {
        reject(approvalRequest, approvalRequest.getReviewedBy());
    }

    @Override
    public void onRejected(ApprovalRequest approvalRequest, User reviewer) {
        reject(approvalRequest, reviewer);
    }

    private void publish(ApprovalRequest approvalRequest, User reviewer) {
        Product product = requireProductForUpdate(approvalRequest.getResourceId());
        requireNotArchived(product);
        ProductRevision revision = requireRevisionForUpdate(product.getId(), approvalRequest.getResourceVersion());
        if (revision.getStatus() != ProductRevisionStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "Product revision is not pending review.");
        }
        validationService.validateStoredRevision(revision);

        ProductRevision previous = product.getCurrentPublishedRevision();
        if (previous != null && !previous.getId().equals(revision.getId())) {
            previous.archive();
            revisionRepository.flush();
        }
        revision.publish(reviewer, Instant.now());
        product.publish(revision);
        auditService.recordSuccessAfterCommit(
                reviewer,
                AuditAction.PRODUCT_REVISION_PUBLISHED,
                AuditTargetType.PRODUCT_REVISION,
                revision.getId(),
                "revision " + revision.getRevisionNumber()
        );
    }

    private void reject(ApprovalRequest approvalRequest, User reviewer) {
        Product product = requireProductForUpdate(approvalRequest.getResourceId());
        ProductRevision revision = requireRevisionForUpdate(product.getId(), approvalRequest.getResourceVersion());
        if (revision.getStatus() != ProductRevisionStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "Product revision is not pending review.");
        }
        revision.reject();
        if (reviewer != null) {
            auditService.recordSuccessAfterCommit(
                    reviewer,
                    AuditAction.PRODUCT_REVISION_REJECTED,
                    AuditTargetType.PRODUCT_REVISION,
                    revision.getId(),
                    "revision " + revision.getRevisionNumber()
            );
        }
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private Product requireProductForUpdate(UUID id) {
        return productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private ProductRevision requireRevision(UUID productId, long revisionNumber) {
        return revisionRepository.findByProductIdAndRevisionNumber(productId, revisionNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product revision not found."));
    }

    private ProductRevision requireRevisionForUpdate(UUID productId, long revisionNumber) {
        return revisionRepository.findByProductAndNumberForUpdate(productId, revisionNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product revision not found."));
    }

    private void requireNotArchived(Product product) {
        if (product.getPublicationStatus() == ProductPublicationStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "Archived products cannot be reviewed.");
        }
    }
}
