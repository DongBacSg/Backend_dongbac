package com.dongbacsaigon.backend.lead.service;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditActor;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.lead.dto.CreateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.LeadPageResponse;
import com.dongbacsaigon.backend.lead.dto.LeadResponse;
import com.dongbacsaigon.backend.lead.dto.PublicContactRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadAssignmentRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadStatusRequest;
import com.dongbacsaigon.backend.lead.entity.CustomerLead;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import com.dongbacsaigon.backend.lead.repository.CustomerLeadRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LeadService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?=.*\\d)[0-9+()\\-\\s]{3,64}$");

    private final CustomerLeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public LeadService(CustomerLeadRepository leadRepository, UserRepository userRepository, AuditService auditService) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LeadResponse create(CreateLeadRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        ResolvedDetails details = resolveDetails(
                request.fullName(), request.phone(), request.email(), request.companyName(), request.message(), request.internalNote()
        );
        CustomerLead lead = new CustomerLead(
                details.fullName(), details.phone(), details.email(), details.companyName(), details.message(), details.internalNote(), actor
        );
        leadRepository.save(lead);
        auditService.recordSuccessAfterCommit(actor, AuditAction.LEAD_CREATED, AuditTargetType.LEAD, lead.getId(), "status NEW");
        return LeadMapper.toResponse(lead);
    }

    @Transactional
    public void createPublicContact(PublicContactRequest request) {
        String name = plainText(request.name());
        String message = plainText(request.message());
        String subject = plainText(request.subject());
        ResolvedDetails details = resolveDetails(name, request.phone(), request.email(), null, message, null);
        if (!StringUtils.hasText(details.message())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contact message is required.");
        }
        subject = requireMaxLength(normalizeOptional(subject), 240, "Contact subject is too long.");
        CustomerLead lead = new CustomerLead(
                details.fullName(), details.phone(), details.email(), null, subject, details.message(), null, null
        );
        leadRepository.save(lead);
        auditService.recordSuccessAfterCommit(
                AuditActor.anonymous(), AuditAction.PUBLIC_CONTACT_CREATED, AuditTargetType.LEAD, lead.getId(), null
        );
    }

    @Transactional(readOnly = true)
    public LeadPageResponse list(
            int page,
            int size,
            String search,
            LeadStatus status,
            UUID assignedTo,
            Boolean unassigned
    ) {
        boolean onlyUnassigned = Boolean.TRUE.equals(unassigned);
        if (assignedTo != null && onlyUnassigned) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "assignedTo and unassigned=true cannot be used together.");
        }
        Page<CustomerLead> leads = leadRepository.findAdminPage(
                normalizeSearch(search),
                status,
                assignedTo,
                onlyUnassigned,
                PageRequest.of(
                        validatePage(page),
                        validateSize(size),
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );
        return new LeadPageResponse(
                leads.getContent().stream().map(LeadMapper::toSummary).toList(),
                leads.getNumber(),
                leads.getSize(),
                leads.getTotalElements(),
                leads.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public LeadResponse get(UUID leadId) {
        return LeadMapper.toResponse(requireLead(leadId));
    }

    @Transactional
    public LeadResponse update(UUID leadId, UpdateLeadRequest request, UUID actorId) {
        CustomerLead lead = requireLead(leadId);
        User actor = requireUser(actorId);
        ResolvedDetails details = resolveDetails(
                request.fullName(), request.phone(), request.email(), request.companyName(), request.message(), request.internalNote()
        );
        lead.updateDetails(
                details.fullName(), details.phone(), details.email(), details.companyName(), details.message(), details.internalNote()
        );
        auditService.recordSuccessAfterCommit(actor, AuditAction.LEAD_UPDATED, AuditTargetType.LEAD, leadId, null);
        return LeadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse updateStatus(UUID leadId, UpdateLeadStatusRequest request, UUID actorId) {
        CustomerLead lead = requireLead(leadId);
        User actor = requireUser(actorId);
        LeadStatus oldStatus = lead.getStatus();
        LeadStatus newStatus = request.status();
        if (newStatus == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Lead status is required.");
        if (oldStatus == newStatus) return LeadMapper.toResponse(lead);
        lead.changeStatus(newStatus);
        auditService.recordSuccessAfterCommit(
                actor, AuditAction.LEAD_STATUS_CHANGED, AuditTargetType.LEAD, leadId, oldStatus + " -> " + newStatus
        );
        return LeadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse updateAssignment(UUID leadId, UpdateLeadAssignmentRequest request, UUID actorId) {
        CustomerLead lead = requireLead(leadId);
        User actor = requireUser(actorId);
        User previous = lead.getAssignedTo();
        User assignee = request.assignedTo() == null ? null : requireEligibleAssignee(request.assignedTo());
        UUID previousId = previous == null ? null : previous.getId();
        UUID assigneeId = assignee == null ? null : assignee.getId();
        if (java.util.Objects.equals(previousId, assigneeId)) return LeadMapper.toResponse(lead);
        lead.assignTo(assignee);
        AuditAction action = assignee == null ? AuditAction.LEAD_UNASSIGNED : AuditAction.LEAD_ASSIGNED;
        String reason = "assignedTo " + (previousId == null ? "none" : previousId) + " -> " + (assigneeId == null ? "none" : assigneeId);
        auditService.recordSuccessAfterCommit(actor, action, AuditTargetType.LEAD, leadId, reason);
        return LeadMapper.toResponse(lead);
    }

    private CustomerLead requireLead(UUID id) {
        return leadRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lead not found."));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private User requireEligibleAssignee(UUID id) {
        User user = requireUser(id);
        if (!user.isActive()) throw new ApiException(HttpStatus.BAD_REQUEST, "Assigned user must be active.");
        return user;
    }

    private ResolvedDetails resolveDetails(
            String fullName,
            String phone,
            String email,
            String companyName,
            String message,
            String internalNote
    ) {
        String normalizedPhone = normalizeOptional(phone);
        if (normalizedPhone != null && !PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone format is invalid.");
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedPhone == null && normalizedEmail == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one phone number or email address is required.");
        }
        return new ResolvedDetails(
                requireMaxLength(normalizeRequired(fullName, "Lead full name is required."), 160, "Lead full name is too long."),
                normalizedPhone,
                normalizedEmail,
                requireMaxLength(normalizeOptional(companyName), 200, "Company name is too long."),
                requireMaxLength(normalizeOptional(message), 5000, "Lead message is too long."),
                requireMaxLength(normalizeOptional(internalNote), 5000, "Lead internal note is too long.")
        );
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.length() > 320) throw new ApiException(HttpStatus.BAD_REQUEST, "Email is too long.");
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email format is invalid.");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) throw new ApiException(HttpStatus.BAD_REQUEST, message);
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String plainText(String value) {
        return value == null ? null : Jsoup.clean(value.trim(), Safelist.none()).trim();
    }

    private String requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) throw new ApiException(HttpStatus.BAD_REQUEST, message);
        return value;
    }

    private String normalizeSearch(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private int validatePage(int page) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0.");
        return page;
    }

    private int validateSize(int size) {
        if (size == 0) return DEFAULT_PAGE_SIZE;
        if (size < 1 || size > MAX_PAGE_SIZE) throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100.");
        return size;
    }

    private record ResolvedDetails(
            String fullName,
            String phone,
            String email,
            String companyName,
            String message,
            String internalNote
    ) {
    }
}
