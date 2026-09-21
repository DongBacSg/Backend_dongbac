package com.dongbacsaigon.backend.audit.service;

public final class AuditRequestContextHolder {

    private static final ThreadLocal<AuditRequestContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private AuditRequestContextHolder() {
    }

    public static void set(AuditRequestContext context) {
        CURRENT_CONTEXT.set(context);
    }

    public static AuditRequestContext get() {
        AuditRequestContext context = CURRENT_CONTEXT.get();
        return context == null ? AuditRequestContext.empty() : context;
    }

    public static void clear() {
        CURRENT_CONTEXT.remove();
    }
}
