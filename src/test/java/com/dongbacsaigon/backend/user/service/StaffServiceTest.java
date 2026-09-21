package com.dongbacsaigon.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.auth.service.PasswordPolicyService;
import com.dongbacsaigon.backend.auth.service.RefreshTokenService;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.dto.StaffAccountResponse;
import com.dongbacsaigon.backend.user.dto.StaffCreateRequest;
import com.dongbacsaigon.backend.user.dto.StaffPasswordResetRequest;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class StaffServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final StaffService staffService = new StaffService(
            userRepository,
            new EmailNormalizer(),
            new FullNameNormalizer(),
            passwordEncoder,
            new PasswordPolicyService(),
            refreshTokenService,
            auditService
    );

    @Test
    void createStaffNormalizesEmailAndAlwaysCreatesStaffRole() {
        UUID adminId = UUID.randomUUID();
        when(passwordEncoder.encode("Temporary1234")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAccountResponse response = staffService.createStaff(
                new StaffCreateRequest(" Staff@Example.com ", " Staff Member ", "Temporary1234"),
                adminId
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("staff@example.com");
        assertThat(response.role()).isEqualTo(UserRole.STAFF);
        assertThat(response.mustChangePassword()).isTrue();
        assertThat(response.createdBy()).isEqualTo(adminId);
        verify(auditService).recordSuccessAfterCommit(
                org.mockito.ArgumentMatchers.nullable(User.class),
                org.mockito.ArgumentMatchers.eq(AuditAction.STAFF_CREATED),
                org.mockito.ArgumentMatchers.eq(AuditTargetType.STAFF),
                org.mockito.ArgumentMatchers.eq(response.id()),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void duplicateStaffEmailReturnsConflict() {
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(true);

        assertThatThrownBy(() -> staffService.createStaff(
                new StaffCreateRequest("staff@example.com", "Staff Member", "Temporary1234"),
                UUID.randomUUID()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("An account with this email already exists.");
    }

    @Test
    void resetPasswordRequiresStaffAndRevokesRefreshTokens() {
        UUID adminId = UUID.randomUUID();
        User staff = User.staff("staff@example.com", "$2a$12$old", "Staff", adminId);
        when(userRepository.findByIdAndRole(staff.getId(), UserRole.STAFF)).thenReturn(Optional.of(staff));
        when(passwordEncoder.encode("Temporary1234")).thenReturn("$2a$12$new");

        StaffAccountResponse response = staffService.resetPassword(
                staff.getId(),
                new StaffPasswordResetRequest("Temporary1234"),
                adminId
        );

        assertThat(response.mustChangePassword()).isTrue();
        assertThat(staff.getPasswordHash()).isEqualTo("$2a$12$new");
        verify(refreshTokenService).revokeAllActiveForUser(staff.getId());
        verify(auditService).recordSuccessAfterCommit(
                org.mockito.ArgumentMatchers.nullable(User.class),
                org.mockito.ArgumentMatchers.eq(AuditAction.STAFF_PASSWORD_RESET),
                org.mockito.ArgumentMatchers.eq(AuditTargetType.STAFF),
                org.mockito.ArgumentMatchers.eq(staff.getId()),
                org.mockito.ArgumentMatchers.isNull()
        );
    }
}
