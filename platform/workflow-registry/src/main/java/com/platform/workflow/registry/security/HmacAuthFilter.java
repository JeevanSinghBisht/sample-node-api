package com.platform.workflow.registry.security;

import com.platform.workflow.registry.config.PlatformProperties;
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

    public static final String TIMESTAMP_HEADER = "X-Platform-Timestamp";
    public static final String SIGNATURE_HEADER = "X-Platform-Signature";

    private final PlatformProperties platformProperties;

    public HmacAuthFilter(PlatformProperties platformProperties) {
        this.platformProperties = platformProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/internal/")) {
            return true;
        }
        return !platformProperties.getInternal().isHmacEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        String timestamp = wrapped.getHeader(TIMESTAMP_HEADER);
        String signature = wrapped.getHeader(SIGNATURE_HEADER);
        String secret = platformProperties.getInternal().getHmacSecret();

        if (timestamp == null || signature == null || secret == null || secret.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing platform auth headers");
            return;
        }

        long skew = platformProperties.getInternal().getMaxTimestampSkewSeconds();
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
        String expected = "hmac-sha256=" + hmacSha256Hex(secret, payload);
        if (!constantTimeEquals(expected, signature.trim())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid signature");
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    static String hmacSha256Hex(String secret, String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
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
