package com.platform.common.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PlatformHmac {

    public static final String TIMESTAMP_HEADER = "X-Platform-Timestamp";
    public static final String SIGNATURE_HEADER = "X-Platform-Signature";

    private PlatformHmac() {}

    public static String sign(String secret, long epochSeconds, String body) {
        String payload = epochSeconds + "." + (body != null ? body : "");
        return "hmac-sha256=" + hmacSha256Hex(secret, payload);
    }

    public static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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
}
