package com.platform.workflow.registry.github;

import com.platform.common.security.PlatformHmac;
import com.platform.workflow.registry.config.PlatformProperties;
import org.springframework.stereotype.Component;

@Component
public class GitHubWebhookVerifier {

    public static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private final PlatformProperties properties;

    public GitHubWebhookVerifier(PlatformProperties properties) {
        this.properties = properties;
    }

    public boolean verify(String payload, String signatureHeader) {
        String secret = properties.getGithub().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expectedHex = signatureHeader.substring("sha256=".length());
        String actualHex = PlatformHmac.hmacSha256Hex(secret, payload);
        return constantTimeEquals(expectedHex, actualHex);
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
