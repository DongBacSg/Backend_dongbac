package com.dongbacsaigon.backend.audit.service;

public record AuditRequestContext(String correlationId, String ipAddress, String userAgent) {

    static AuditRequestContext empty() {
        return new AuditRequestContext(null, null, null);
    }
}
