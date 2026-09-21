package com.dongbacsaigon.backend.approval.repository;

import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRequestRepository
        extends JpaRepository<ApprovalRequest, UUID>, JpaSpecificationExecutor<ApprovalRequest> {

    long countByStatus(ApprovalRequestStatus status);
    long countByStatusAndSubmittedById(ApprovalRequestStatus status, UUID submittedById);

    boolean existsByResourceTypeAndResourceIdAndResourceVersionAndStatus(
            ApprovalResourceType resourceType,
            UUID resourceId,
            long resourceVersion,
            ApprovalRequestStatus status
    );

    boolean existsByResourceTypeAndResourceId(ApprovalResourceType resourceType, UUID resourceId);

    @EntityGraph(attributePaths = {"submittedBy", "reviewedBy"})
    Optional<ApprovalRequest> findDetailedById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select approvalRequest
            from ApprovalRequest approvalRequest
            join fetch approvalRequest.submittedBy
            left join fetch approvalRequest.reviewedBy
            where approvalRequest.id = :id
            """)
    Optional<ApprovalRequest> findByIdForUpdate(@Param("id") UUID id);
}
