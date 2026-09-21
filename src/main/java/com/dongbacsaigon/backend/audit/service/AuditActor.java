package com.dongbacsaigon.backend.audit.service;

import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;

public record AuditActor(UUID userId, String emailSnapshot, String roleSnapshot) {

    public static AuditActor from(User user) {
        if (user == null) {
            return anonymous();
        }
        UserRole role = user.getRole();
        return new AuditActor(
                user.getId(),
                user.getEmail(),
                role == null ? null : role.name()
        );
    }

    public static AuditActor anonymous() {
        return new AuditActor(null, null, null);
    }

    public static AuditActor unknownEmail(String emailSnapshot) {
        return new AuditActor(null, emailSnapshot, null);
    }
}
