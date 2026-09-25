package com.votacao.entrypoint.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MDCRequestFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final String MDC_REQUEST_METHOD_KEY = "requestMethod";
    public static final String MDC_REQUEST_URI_KEY = "requestURI";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
            MDC.put(MDC_REQUEST_METHOD_KEY, request.getMethod());
            MDC.put(MDC_REQUEST_URI_KEY, request.getRequestURI());

            log.debug("Request received, correlationId: {}", correlationId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
            MDC.remove(MDC_REQUEST_METHOD_KEY);
            MDC.remove(MDC_REQUEST_URI_KEY);
        }
    }

}
