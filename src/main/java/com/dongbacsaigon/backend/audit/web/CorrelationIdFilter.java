package com.dongbacsaigon.backend.audit.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import com.dongbacsaigon.backend.audit.service.AuditRequestContext;
import com.dongbacsaigon.backend.audit.service.AuditRequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        AuditRequestContextHolder.set(new AuditRequestContext(
                correlationId,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        ));
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditRequestContextHolder.clear();
            MDC.remove("correlationId");
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String incomingCorrelationId = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(incomingCorrelationId)
                && SAFE_CORRELATION_ID.matcher(incomingCorrelationId.trim()).matches()) {
            return incomingCorrelationId.trim();
        }
        return UUID.randomUUID().toString();
    }
}
