package com.platform.cursor.bridge.security;

import com.platform.common.security.PlatformHmac;
import com.platform.cursor.bridge.config.BridgeProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HmacAuthFilter extends OncePerRequestFilter {

    private final BridgeProperties properties;

    public HmacAuthFilter(BridgeProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!requiresAuth(request.getRequestURI())) {
            return true;
        }
        return !properties.getInternal().isHmacEnabled();
    }

    private boolean requiresAuth(String uri) {
        return uri.startsWith("/internal/") || "/agents".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        String timestamp = wrapped.getHeader(PlatformHmac.TIMESTAMP_HEADER);
        String signature = wrapped.getHeader(PlatformHmac.SIGNATURE_HEADER);
        String secret = properties.getInternal().getHmacSecret();

        if (timestamp == null || signature == null || secret == null || secret.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing platform auth headers");
            return;
        }

        long skew = properties.getInternal().getMaxTimestampSkewSeconds();
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid timestamp");
            return;
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > skew) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Timestamp skew too large");
            return;
        }

        String body = new String(wrapped.getCachedBody(), StandardCharsets.UTF_8);
        String payload = timestamp + "." + body;
        String expected = "hmac-sha256=" + PlatformHmac.hmacSha256Hex(secret, payload);
        if (!constantTimeEquals(expected, signature.trim())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid signature");
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
