package com.dongbacsaigon.backend.auth.service;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditActor;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import com.dongbacsaigon.backend.user.service.EmailNormalizer;
import com.dongbacsaigon.backend.user.service.FullNameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final AuthProperties authProperties;
    private final EmailNormalizer emailNormalizer;
    private final FullNameNormalizer fullNameNormalizer;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    AdminBootstrapService(
            UserRepository userRepository,
            AuthProperties authProperties,
            EmailNormalizer emailNormalizer,
            FullNameNormalizer fullNameNormalizer,
            PasswordPolicyService passwordPolicyService,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.authProperties = authProperties;
        this.emailNormalizer = emailNormalizer;
        this.fullNameNormalizer = fullNameNormalizer;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public void bootstrapIfNecessary() {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            LOGGER.info("ADMIN bootstrap skipped because an ADMIN account already exists.");
            return;
        }

        authProperties.bootstrapAdmin()
                .ifPresentOrElse(this::createInitialAdmin, this::logMissingBootstrapConfiguration);
    }

    private void createInitialAdmin(AuthProperties.BootstrapAdmin bootstrapAdmin) {
        try {
            String normalizedEmail = emailNormalizer.normalize(bootstrapAdmin.email());
            String normalizedName = fullNameNormalizer.normalize(bootstrapAdmin.name());
            passwordPolicyService.validate(bootstrapAdmin.password());

            if (userRepository.existsByEmail(normalizedEmail)) {
                LOGGER.warn("ADMIN bootstrap skipped because the configured email already belongs to another account.");
                return;
            }

            User admin = User.admin(
                    normalizedEmail,
                    passwordEncoder.encode(bootstrapAdmin.password()),
                    normalizedName
            );
            userRepository.save(admin);
            LOGGER.info("Bootstrapped first ADMIN account {}", admin.getId());
            auditService.recordSuccessAfterCommit(
                    AuditActor.anonymous(),
                    AuditAction.ADMIN_BOOTSTRAPPED,
                    AuditTargetType.USER,
                    admin.getId(),
                    "Initial ADMIN bootstrap."
            );
        } catch (ApiException exception) {
            LOGGER.warn("ADMIN bootstrap skipped because supplied bootstrap values are invalid.");
        }
    }

    private void logMissingBootstrapConfiguration() {
        if (authProperties.hasAnyBootstrapAdminValue()) {
            LOGGER.warn("ADMIN bootstrap skipped because bootstrap configuration is incomplete.");
        } else {
            LOGGER.warn("No ADMIN account exists and bootstrap configuration was not supplied.");
        }
    }
}
