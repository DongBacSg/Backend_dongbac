package com.dongbacsaigon.backend.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.dto.ApprovalApproveRequest;
import com.dongbacsaigon.backend.approval.dto.ApprovalRejectRequest;
import com.dongbacsaigon.backend.approval.dto.ApprovalRequestResponse;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    private final ApprovalRequestRepository approvalRequestRepository = mock(ApprovalRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TestApprovalResourceHandler resourceHandler = new TestApprovalResourceHandler(false);
    private final AuditService auditService = mock(AuditService.class);
    private final ApprovalService approvalService = new ApprovalService(
            approvalRequestRepository,
            userRepository,
            List.of(resourceHandler),
            auditService
    );

    @Test
    void submitForReviewCreatesPendingRequest() {
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", UUID.randomUUID());
        UUID resourceId = UUID.randomUUID();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestResponse response = approvalService.submitForReview(
                ApprovalResourceType.PRODUCT,
                resourceId,
                1,
                staff.getId()
        );

        assertThat(response.status()).isEqualTo(ApprovalRequestStatus.PENDING);
        assertThat(response.resourceType()).isEqualTo(ApprovalResourceType.PRODUCT);
        assertThat(response.resourceId()).isEqualTo(resourceId);
        assertThat(response.resourceVersion()).isEqualTo(1);
        assertThat(resourceHandler.validateCalled).isTrue();
        verify(approvalRequestRepository).save(any(ApprovalRequest.class));
    }

    @Test
    void duplicatePendingRequestIsRejectedBeforeSave() {
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", UUID.randomUUID());
        UUID resourceId = UUID.randomUUID();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(approvalRequestRepository.existsByResourceTypeAndResourceIdAndResourceVersionAndStatus(
                ApprovalResourceType.ARTICLE,
                resourceId,
                2,
                ApprovalRequestStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() -> approvalService.submitForReview(
                ApprovalResourceType.ARTICLE,
                resourceId,
                2,
                staff.getId()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("A pending approval request already exists for this resource version.");
    }

    @Test
    void approvePendingRequestCallsHandlerAndRecordsReviewer() {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", admin.getId());
        ApprovalRequest approvalRequest = new ApprovalRequest(
                ApprovalResourceType.PRODUCT,
                UUID.randomUUID(),
                1,
                staff,
                Instant.now()
        );
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(approvalRequestRepository.findByIdForUpdate(approvalRequest.getId())).thenReturn(Optional.of(approvalRequest));

        ApprovalRequestResponse response = approvalService.approve(
                approvalRequest.getId(),
                new ApprovalApproveRequest("Ready"),
                admin.getId()
        );

        assertThat(resourceHandler.approvedCalled).isTrue();
        assertThat(response.status()).isEqualTo(ApprovalRequestStatus.APPROVED);
        assertThat(response.reviewedBy().id()).isEqualTo(admin.getId());
        assertThat(response.reviewedAt()).isNotNull();
        assertThat(response.reviewNote()).isEqualTo("Ready");
    }

    @Test
    void alreadyApprovedRequestCannotBeApprovedAgain() {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", admin.getId());
        ApprovalRequest approvalRequest = new ApprovalRequest(
                ApprovalResourceType.PRODUCT,
                UUID.randomUUID(),
                1,
                staff,
                Instant.now()
        );
        approvalRequest.approve(admin, Instant.now(), null);
        when(approvalRequestRepository.findByIdForUpdate(approvalRequest.getId())).thenReturn(Optional.of(approvalRequest));

        assertThatThrownBy(() -> approvalService.approve(
                approvalRequest.getId(),
                new ApprovalApproveRequest(null),
                admin.getId()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Approval request has already been reviewed.");
    }

    @Test
    void rejectRequiresMeaningfulReasonAndStoresReason() {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", admin.getId());
        ApprovalRequest approvalRequest = new ApprovalRequest(
                ApprovalResourceType.ARTICLE,
                UUID.randomUUID(),
                1,
                staff,
                Instant.now()
        );
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(approvalRequestRepository.findByIdForUpdate(approvalRequest.getId())).thenReturn(Optional.of(approvalRequest));

        ApprovalRequestResponse response = approvalService.reject(
                approvalRequest.getId(),
                new ApprovalRejectRequest("Needs clearer product specs"),
                admin.getId()
        );

        assertThat(resourceHandler.rejectedCalled).isTrue();
        assertThat(response.status()).isEqualTo(ApprovalRequestStatus.REJECTED);
        assertThat(response.reviewNote()).isEqualTo("Needs clearer product specs");
    }

    @Test
    void handlerFailureLeavesRequestPending() {
        TestApprovalResourceHandler failingHandler = new TestApprovalResourceHandler(true);
        ApprovalService service = new ApprovalService(
                approvalRequestRepository,
                userRepository,
                List.of(failingHandler),
                auditService
        );
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", admin.getId());
        ApprovalRequest approvalRequest = new ApprovalRequest(
                ApprovalResourceType.PRODUCT,
                UUID.randomUUID(),
                1,
                staff,
                Instant.now()
        );
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(approvalRequestRepository.findByIdForUpdate(approvalRequest.getId())).thenReturn(Optional.of(approvalRequest));

        assertThatThrownBy(() -> service.approve(
                approvalRequest.getId(),
                new ApprovalApproveRequest("Ready"),
                admin.getId()
        ))
                .isInstanceOf(IllegalStateException.class);
        assertThat(approvalRequest.getStatus()).isEqualTo(ApprovalRequestStatus.PENDING);
    }

    private static final class TestApprovalResourceHandler implements ApprovalResourceHandler {

        private final boolean failDecision;
        private boolean validateCalled;
        private boolean approvedCalled;
        private boolean rejectedCalled;

        private TestApprovalResourceHandler(boolean failDecision) {
            this.failDecision = failDecision;
        }

        @Override
        public boolean supports(ApprovalResourceType resourceType) {
            return resourceType == ApprovalResourceType.PRODUCT || resourceType == ApprovalResourceType.ARTICLE;
        }

        @Override
        public void validateCanSubmit(
                ApprovalResourceType resourceType,
                UUID resourceId,
                long resourceVersion,
                UUID submittedBy
        ) {
            validateCalled = true;
        }

        @Override
        public void onApproved(ApprovalRequest approvalRequest) {
            approvedCalled = true;
            if (failDecision) {
                throw new IllegalStateException("handler failed");
            }
        }

        @Override
        public void onRejected(ApprovalRequest approvalRequest) {
            rejectedCalled = true;
            if (failDecision) {
                throw new IllegalStateException("handler failed");
            }
        }
    }
}
