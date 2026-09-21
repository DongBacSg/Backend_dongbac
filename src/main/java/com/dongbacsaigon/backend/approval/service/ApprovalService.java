package com.dongbacsaigon.backend.approval.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.dto.ApprovalApproveRequest;
import com.dongbacsaigon.backend.approval.dto.ApprovalPageResponse;
import com.dongbacsaigon.backend.approval.dto.ApprovalRejectRequest;
import com.dongbacsaigon.backend.approval.dto.ApprovalRequestResponse;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApprovalService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final List<ApprovalResourceHandler> resourceHandlers;
    private final AuditService auditService;

    public ApprovalService(
            ApprovalRequestRepository approvalRequestRepository,
            UserRepository userRepository,
            List<ApprovalResourceHandler> resourceHandlers,
            AuditService auditService
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.userRepository = userRepository;
        this.resourceHandlers = resourceHandlers;
        this.auditService = auditService;
    }

    @Transactional
    public ApprovalRequestResponse submitForReview(
            ApprovalResourceType resourceType,
            UUID resourceId,
            long resourceVersion,
            UUID submittedBy
    ) {
        validateResourceInput(resourceType, resourceId, resourceVersion);
        User submitter = requireUser(submittedBy);
        ApprovalResourceHandler handler = requireHandler(resourceType);
        handler.validateCanSubmit(resourceType, resourceId, resourceVersion, submittedBy);

        if (approvalRequestRepository.existsByResourceTypeAndResourceIdAndResourceVersionAndStatus(
                resourceType,
                resourceId,
                resourceVersion,
                ApprovalRequestStatus.PENDING
        )) {
            throw new ApiException(HttpStatus.CONFLICT, "A pending approval request already exists for this resource version.");
        }

        ApprovalRequest approvalRequest = new ApprovalRequest(
                resourceType,
                resourceId,
                resourceVersion,
                submitter,
                Instant.now()
        );

        try {
            ApprovalRequest savedRequest = approvalRequestRepository.save(approvalRequest);
            auditService.recordSuccessAfterCommit(
                    submitter,
                    AuditAction.APPROVAL_SUBMITTED,
                    AuditTargetType.APPROVAL_REQUEST,
                    savedRequest.getId(),
                    safeResourceReference(savedRequest)
            );
            return ApprovalRequestMapper.toResponse(savedRequest);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "A pending approval request already exists for this resource version.");
        }
    }

    @Transactional(readOnly = true)
    public ApprovalPageResponse listApprovals(
            int page,
            int size,
            ApprovalRequestStatus status,
            ApprovalResourceType resourceType,
            UUID submittedBy,
            Instant dateFrom,
            Instant dateTo
    ) {
        return toPageResponse(approvalRequestRepository.findAll(
                specification(status, resourceType, submittedBy, dateFrom, dateTo),
                pageRequest(page, size)
        ));
    }

    @Transactional(readOnly = true)
    public ApprovalPageResponse listMine(
            UUID currentUserId,
            int page,
            int size,
            ApprovalRequestStatus status,
            ApprovalResourceType resourceType
    ) {
        return toPageResponse(approvalRequestRepository.findAll(
                specification(status, resourceType, currentUserId, null, null),
                pageRequest(page, size)
        ));
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getApproval(UUID id) {
        return approvalRequestRepository.findDetailedById(id)
                .map(ApprovalRequestMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Approval request not found."));
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getMineOrAdmin(UUID id, UUID currentUserId, boolean admin) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Approval request not found."));

        if (!admin && !approvalRequest.getSubmittedBy().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden.");
        }

        return ApprovalRequestMapper.toResponse(approvalRequest);
    }

    @Transactional
    public ApprovalRequestResponse approve(UUID id, ApprovalApproveRequest request, UUID adminId) {
        ApprovalRequest approvalRequest = requirePendingForDecision(id);
        User admin = requireAdmin(adminId);
        ApprovalResourceHandler handler = requireHandler(approvalRequest.getResourceType());
        handler.onApproved(approvalRequest, admin);

        approvalRequest.approve(admin, Instant.now(), normalizeOptionalNote(request == null ? null : request.note()));
        auditService.recordSuccessAfterCommit(
                admin,
                AuditAction.APPROVAL_APPROVED,
                AuditTargetType.APPROVAL_REQUEST,
                approvalRequest.getId(),
                safeResourceReference(approvalRequest)
        );
        return ApprovalRequestMapper.toResponse(approvalRequest);
    }

    @Transactional
    public ApprovalRequestResponse reject(UUID id, ApprovalRejectRequest request, UUID adminId) {
        ApprovalRequest approvalRequest = requirePendingForDecision(id);
        User admin = requireAdmin(adminId);
        String reason = normalizeRequiredReason(request.reason());
        ApprovalResourceHandler handler = requireHandler(approvalRequest.getResourceType());
        handler.onRejected(approvalRequest, admin);

        approvalRequest.reject(admin, Instant.now(), reason);
        auditService.recordSuccessAfterCommit(
                admin,
                AuditAction.APPROVAL_REJECTED,
                AuditTargetType.APPROVAL_REQUEST,
                approvalRequest.getId(),
                reason
        );
        return ApprovalRequestMapper.toResponse(approvalRequest);
    }

    private ApprovalRequest requirePendingForDecision(UUID id) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Approval request not found."));

        if (!approvalRequest.isPending()) {
            throw new ApiException(HttpStatus.CONFLICT, "Approval request has already been reviewed.");
        }

        return approvalRequest;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private User requireAdmin(UUID userId) {
        User user = requireUser(userId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden.");
        }
        return user;
    }

    private ApprovalResourceHandler requireHandler(ApprovalResourceType resourceType) {
        return resourceHandlers.stream()
                .filter(handler -> handler.supports(resourceType))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "No approval resource handler is registered for " + resourceType + "."
                ));
    }

    private void validateResourceInput(ApprovalResourceType resourceType, UUID resourceId, long resourceVersion) {
        if (resourceType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resource type is required.");
        }
        if (resourceId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resource id is required.");
        }
        if (resourceVersion < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resource version must be at least 1.");
        }
    }

    private String normalizeOptionalNote(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        return note.trim();
    }

    private String normalizeRequiredReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rejection reason is required.");
        }
        String trimmedReason = reason.trim();
        if (trimmedReason.length() < 3 || trimmedReason.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rejection reason must be between 3 and 1000 characters.");
        }
        return trimmedReason;
    }

    private ApprovalPageResponse toPageResponse(Page<ApprovalRequest> approvalPage) {
        Page<ApprovalRequestResponse> responsePage = approvalPage.map(ApprovalRequestMapper::toResponse);
        return new ApprovalPageResponse(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages()
        );
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "submittedAt")
        );
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

    private Specification<ApprovalRequest> specification(
            ApprovalRequestStatus status,
            ApprovalResourceType resourceType,
            UUID submittedBy,
            Instant dateFrom,
            Instant dateTo
    ) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            if (resourceType != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("resourceType"), resourceType));
            }
            if (submittedBy != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("submittedBy").get("id"), submittedBy));
            }
            if (dateFrom != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("submittedAt"), dateFrom));
            }
            if (dateTo != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("submittedAt"), dateTo));
            }
            return predicate;
        };
    }

    private String safeResourceReference(ApprovalRequest approvalRequest) {
        return approvalRequest.getResourceType()
                + " "
                + approvalRequest.getResourceId()
                + " v"
                + approvalRequest.getResourceVersion();
    }
}
