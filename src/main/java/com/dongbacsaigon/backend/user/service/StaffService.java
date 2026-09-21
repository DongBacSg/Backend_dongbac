package com.dongbacsaigon.backend.user.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.auth.service.PasswordPolicyService;
import com.dongbacsaigon.backend.auth.service.RefreshTokenService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.dto.StaffAccountResponse;
import com.dongbacsaigon.backend.user.dto.StaffCreateRequest;
import com.dongbacsaigon.backend.user.dto.StaffPageResponse;
import com.dongbacsaigon.backend.user.dto.StaffPasswordResetRequest;
import com.dongbacsaigon.backend.user.dto.StaffUpdateRequest;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.entity.UserStatus;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StaffService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaffService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final FullNameNormalizer fullNameNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public StaffService(
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            FullNameNormalizer fullNameNormalizer,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            RefreshTokenService refreshTokenService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.fullNameNormalizer = fullNameNormalizer;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public StaffPageResponse listStaff(int page, int size, String search, UserStatus status) {
        PageRequest pageRequest = PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.ASC, "email")
        );
        String normalizedSearch = normalizeSearch(search);
        Page<StaffAccountResponse> staffPage = userRepository.searchByRole(
                        UserRole.STAFF,
                        status,
                        normalizedSearch,
                        pageRequest
                )
                .map(UserResponseMapper::toStaffAccount);

        return new StaffPageResponse(
                staffPage.getContent(),
                staffPage.getNumber(),
                staffPage.getSize(),
                staffPage.getTotalElements(),
                staffPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public StaffAccountResponse getStaff(UUID staffId) {
        return UserResponseMapper.toStaffAccount(requireStaff(staffId));
    }

    @Transactional
    public StaffAccountResponse createStaff(StaffCreateRequest request, UUID adminId) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        String normalizedFullName = fullNameNormalizer.normalize(request.fullName());
        passwordPolicyService.validate(request.temporaryPassword());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw duplicateEmail();
        }

        User staff = User.staff(
                normalizedEmail,
                passwordEncoder.encode(request.temporaryPassword()),
                normalizedFullName,
                adminId
        );

        try {
            User savedStaff = userRepository.save(staff);
            LOGGER.info("STAFF account {} created by admin {}", savedStaff.getId(), adminId);
            auditService.recordSuccessAfterCommit(
                    actor(adminId),
                    AuditAction.STAFF_CREATED,
                    AuditTargetType.STAFF,
                    savedStaff.getId(),
                    null
            );
            return UserResponseMapper.toStaffAccount(savedStaff);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }
    }

    @Transactional
    public StaffAccountResponse updateStaff(UUID staffId, StaffUpdateRequest request, UUID adminId) {
        User staff = requireStaff(staffId);
        String email = staff.getEmail();
        String fullName = staff.getFullName();

        if (request.email() != null) {
            email = emailNormalizer.normalize(request.email());
            if (!email.equals(staff.getEmail()) && userRepository.existsByEmail(email)) {
                throw duplicateEmail();
            }
        }

        if (request.fullName() != null) {
            fullName = fullNameNormalizer.normalize(request.fullName());
        }

        staff.updateProfile(email, fullName);
        LOGGER.info("STAFF account {} updated by admin {}", staff.getId(), adminId);
        auditService.recordSuccessAfterCommit(
                actor(adminId),
                AuditAction.STAFF_UPDATED,
                AuditTargetType.STAFF,
                staff.getId(),
                null
        );
        return UserResponseMapper.toStaffAccount(staff);
    }

    @Transactional
    public StaffAccountResponse lockStaff(UUID staffId, UUID adminId) {
        User staff = requireStaff(staffId);
        staff.lock();
        refreshTokenService.revokeAllActiveForUser(staff.getId());
        LOGGER.info("STAFF account {} locked by admin {}", staff.getId(), adminId);
        auditService.recordSuccessAfterCommit(
                actor(adminId),
                AuditAction.STAFF_LOCKED,
                AuditTargetType.STAFF,
                staff.getId(),
                null
        );
        return UserResponseMapper.toStaffAccount(staff);
    }

    @Transactional
    public StaffAccountResponse unlockStaff(UUID staffId, UUID adminId) {
        User staff = requireStaff(staffId);
        staff.unlock();
        LOGGER.info("STAFF account {} unlocked by admin {}", staff.getId(), adminId);
        auditService.recordSuccessAfterCommit(
                actor(adminId),
                AuditAction.STAFF_UNLOCKED,
                AuditTargetType.STAFF,
                staff.getId(),
                null
        );
        return UserResponseMapper.toStaffAccount(staff);
    }

    @Transactional
    public StaffAccountResponse resetPassword(UUID staffId, StaffPasswordResetRequest request, UUID adminId) {
        User staff = requireStaff(staffId);
        passwordPolicyService.validate(request.temporaryPassword());
        staff.changePassword(passwordEncoder.encode(request.temporaryPassword()), true);
        staff.resetLoginLock();
        refreshTokenService.revokeAllActiveForUser(staff.getId());
        LOGGER.info("STAFF account {} password reset by admin {}", staff.getId(), adminId);
        auditService.recordSuccessAfterCommit(
                actor(adminId),
                AuditAction.STAFF_PASSWORD_RESET,
                AuditTargetType.STAFF,
                staff.getId(),
                null
        );
        return UserResponseMapper.toStaffAccount(staff);
    }

    private User requireStaff(UUID staffId) {
        return userRepository.findByIdAndRole(staffId, UserRole.STAFF)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff account not found."));
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

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException duplicateEmail() {
        return new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
    }

    private User actor(UUID adminId) {
        return Optional.ofNullable(adminId)
                .flatMap(userRepository::findById)
                .orElse(null);
    }
}
