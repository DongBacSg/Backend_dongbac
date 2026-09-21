package com.dongbacsaigon.backend.audit.service;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.dto.AuditLogResponse;
import com.dongbacsaigon.backend.audit.dto.AuditPageResponse;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditLog;
import com.dongbacsaigon.backend.audit.entity.AuditOutcome;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.repository.AuditLogRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AuditPageResponse listAuditLogs(
            int page,
            int size,
            AuditAction action,
            UUID actorUserId,
            AuditTargetType targetType,
            UUID targetId,
            AuditOutcome outcome,
            Instant dateFrom,
            Instant dateTo
    ) {
        PageRequest pageRequest = PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        Page<AuditLogResponse> auditPage = auditLogRepository.findAll(
                        specification(action, actorUserId, targetType, targetId, outcome, dateFrom, dateTo),
                        pageRequest
                )
                .map(AuditLogMapper::toResponse);

        return new AuditPageResponse(
                auditPage.getContent(),
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLog(UUID id) {
        return auditLogRepository.findById(id)
                .map(AuditLogMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Audit log not found."));
    }

    private Specification<AuditLog> specification(
            AuditAction action,
            UUID actorUserId,
            AuditTargetType targetType,
            UUID targetId,
            AuditOutcome outcome,
            Instant dateFrom,
            Instant dateTo
    ) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (action != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("action"), action));
            }
            if (actorUserId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("actorUserId"), actorUserId));
            }
            if (targetType != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("targetType"), targetType));
            }
            if (targetId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("targetId"), targetId));
            }
            if (outcome != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("outcome"), outcome));
            }
            if (dateFrom != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), dateFrom));
            }
            if (dateTo != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), dateTo));
            }
            return predicate;
        };
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
