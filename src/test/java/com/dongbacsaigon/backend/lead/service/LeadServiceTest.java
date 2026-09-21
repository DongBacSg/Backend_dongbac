package com.dongbacsaigon.backend.lead.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.lead.dto.CreateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.LeadPageResponse;
import com.dongbacsaigon.backend.lead.dto.LeadResponse;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadAssignmentRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadStatusRequest;
import com.dongbacsaigon.backend.lead.entity.CustomerLead;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import com.dongbacsaigon.backend.lead.repository.CustomerLeadRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class LeadServiceTest {

    private final CustomerLeadRepository leadRepository = mock(CustomerLeadRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final LeadService service = new LeadService(leadRepository, userRepository, auditService);

    @Test
    void createPhoneOnlyLeadStartsNewAndDerivesCreatedBy() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(leadRepository.save(any(CustomerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadResponse response = service.create(
                new CreateLeadRequest("  Customer  ", " +84 (912) 345-678 ", null, null, null, "Internal"),
                staff.getId()
        );

        assertThat(response.fullName()).isEqualTo("Customer");
        assertThat(response.phone()).isEqualTo("+84 (912) 345-678");
        assertThat(response.email()).isNull();
        assertThat(response.status()).isEqualTo(LeadStatus.NEW);
        assertThat(response.createdBy().id()).isEqualTo(staff.getId());
        verify(auditService).recordSuccessAfterCommit(
                staff, AuditAction.LEAD_CREATED, AuditTargetType.LEAD, response.id(), "status NEW"
        );
    }

    @Test
    void createEmailOnlyLeadTrimsAndNormalizesEmail() {
        User admin = admin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(leadRepository.save(any(CustomerLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadResponse response = service.create(
                new CreateLeadRequest("Customer", null, "  PERSON@Example.COM ", "Company", null, null),
                admin.getId()
        );

        assertThat(response.email()).isEqualTo("person@example.com");
        assertThat(response.phone()).isNull();
    }

    @Test
    void createRejectsMissingContactAndInvalidEmail() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.create(
                new CreateLeadRequest("Customer", null, " ", null, null, null), staff.getId()
        )).isInstanceOf(ApiException.class).hasMessage("At least one phone number or email address is required.");

        assertThatThrownBy(() -> service.create(
                new CreateLeadRequest("Customer", null, "not-an-email", null, null, null), staff.getId()
        )).isInstanceOf(ApiException.class).hasMessage("Email format is invalid.");
    }

    @Test
    void updateKeepsStatusAndAssignmentOutsideGeneralEditAndDoesNotAuditPii() {
        User actor = staff();
        User assignee = admin();
        CustomerLead lead = lead(actor);
        lead.assignTo(assignee);
        lead.changeStatus(LeadStatus.CONTACTED);
        stubLead(lead, actor);

        LeadResponse response = service.update(
                lead.getId(),
                new UpdateLeadRequest("Updated", null, "new@example.com", "Company", "Sensitive message", "Sensitive note"),
                actor.getId()
        );

        assertThat(response.status()).isEqualTo(LeadStatus.CONTACTED);
        assertThat(response.assignedTo().id()).isEqualTo(assignee.getId());
        verify(auditService).recordSuccessAfterCommit(
                actor, AuditAction.LEAD_UPDATED, AuditTargetType.LEAD, lead.getId(), null
        );
    }

    @Test
    void statusUpdateAllowsAnyValidStatusAndSkipsNoOpAudit() {
        User actor = staff();
        CustomerLead lead = lead(actor);
        stubLead(lead, actor);

        LeadResponse changed = service.updateStatus(
                lead.getId(), new UpdateLeadStatusRequest(LeadStatus.COMPLETED), actor.getId()
        );
        assertThat(changed.status()).isEqualTo(LeadStatus.COMPLETED);
        verify(auditService).recordSuccessAfterCommit(
                actor, AuditAction.LEAD_STATUS_CHANGED, AuditTargetType.LEAD, lead.getId(), "NEW -> COMPLETED"
        );

        service.updateStatus(lead.getId(), new UpdateLeadStatusRequest(LeadStatus.COMPLETED), actor.getId());
        verify(auditService, org.mockito.Mockito.times(1)).recordSuccessAfterCommit(
                actor, AuditAction.LEAD_STATUS_CHANGED, AuditTargetType.LEAD, lead.getId(), "NEW -> COMPLETED"
        );
    }

    @Test
    void assignmentSupportsAssignReassignAndUnassignWithSafeAuditMetadata() {
        User actor = admin();
        User first = staff();
        User second = User.staff("second@example.com", "$2a$12$hash", "Second", actor.getId());
        CustomerLead lead = lead(actor);
        stubLead(lead, actor);
        when(userRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(userRepository.findById(second.getId())).thenReturn(Optional.of(second));

        service.updateAssignment(lead.getId(), new UpdateLeadAssignmentRequest(first.getId()), actor.getId());
        service.updateAssignment(lead.getId(), new UpdateLeadAssignmentRequest(second.getId()), actor.getId());
        LeadResponse unassigned = service.updateAssignment(lead.getId(), new UpdateLeadAssignmentRequest(null), actor.getId());

        assertThat(unassigned.assignedTo()).isNull();
        verify(auditService).recordSuccessAfterCommit(
                actor, AuditAction.LEAD_UNASSIGNED, AuditTargetType.LEAD, lead.getId(),
                "assignedTo " + second.getId() + " -> none"
        );
    }

    @Test
    void assignmentRejectsLockedUser() {
        User actor = admin();
        User locked = staff();
        locked.lock();
        CustomerLead lead = lead(actor);
        stubLead(lead, actor);
        when(userRepository.findById(locked.getId())).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.updateAssignment(
                lead.getId(), new UpdateLeadAssignmentRequest(locked.getId()), actor.getId()
        )).isInstanceOf(ApiException.class).hasMessage("Assigned user must be active.");
    }

    @Test
    void listUsesFiltersStablePaginationAndRejectsConflictingAssignmentFilters() {
        User actor = admin();
        CustomerLead lead = lead(actor);
        when(leadRepository.findAdminPage(
                eq("company"), eq(LeadStatus.NEW), eq(actor.getId()), eq(false), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(lead)));

        LeadPageResponse response = service.list(0, 20, "  COMPANY ", LeadStatus.NEW, actor.getId(), false);

        assertThat(response.content()).hasSize(1);
        verify(leadRepository).findAdminPage(
                eq("company"), eq(LeadStatus.NEW), eq(actor.getId()), eq(false), any(Pageable.class)
        );
        assertThatThrownBy(() -> service.list(0, 20, null, null, actor.getId(), true))
                .isInstanceOf(ApiException.class)
                .hasMessage("assignedTo and unassigned=true cannot be used together.");
        assertThatThrownBy(() -> service.list(0, 101, null, null, null, false))
                .isInstanceOf(ApiException.class)
                .hasMessage("Size must be between 1 and 100.");
    }

    @Test
    void leadUsesJpaOptimisticVersioningAndExactStatuses() throws Exception {
        Field version = CustomerLead.class.getDeclaredField("version");
        assertThat(version.getAnnotation(Version.class)).isNotNull();
        assertThat(LeadStatus.values()).containsExactly(
                LeadStatus.NEW,
                LeadStatus.CONTACTED,
                LeadStatus.IN_PROGRESS,
                LeadStatus.COMPLETED,
                LeadStatus.CANCELLED,
                LeadStatus.SPAM
        );
    }

    private void stubLead(CustomerLead lead, User actor) {
        when(leadRepository.findDetailedById(lead.getId())).thenReturn(Optional.of(lead));
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
    }

    private CustomerLead lead(User creator) {
        return new CustomerLead("Customer", "0901234567", null, "Company", "Message", "Note", creator);
    }

    private User admin() { return User.admin("admin@example.com", "$2a$12$hash", "Admin"); }
    private User staff() { return User.staff("staff@example.com", "$2a$12$hash", "Staff", UUID.randomUUID()); }
}
