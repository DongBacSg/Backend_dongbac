package com.dongbacsaigon.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import com.dongbacsaigon.backend.user.service.EmailNormalizer;
import com.dongbacsaigon.backend.user.service.FullNameNormalizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditService auditService = mock(AuditService.class);

    @Test
    void existingAdminIsNeverResetOrDuplicated() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        service().bootstrapIfNecessary();

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsInitialAdminOnlyWhenNoAdminExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().bootstrapIfNecessary();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userCaptor.getValue().isMustChangePassword()).isFalse();
    }

    @Test
    void existingEmailPreventsDuplicateBootstrapAccount() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        service().bootstrapIfNecessary();

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    private AdminBootstrapService service() {
        return new AdminBootstrapService(
                userRepository,
                new AuthProperties(
                        7,
                        "dbsg_refresh",
                        false,
                        "Lax",
                        5,
                        15,
                        " Admin@Example.com ",
                        "StrongPassword123!",
                        " Initial Admin "
                ),
                new EmailNormalizer(),
                new FullNameNormalizer(),
                new PasswordPolicyService(),
                passwordEncoder,
                auditService
        );
    }
}
