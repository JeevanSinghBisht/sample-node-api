package com.platform.workflow.registry.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HmacAuthFilterTest {

    @Test
    void hmac_isDeterministic() {
        String a = HmacAuthFilter.hmacSha256Hex("secret", "1730534400.{}");
        String b = HmacAuthFilter.hmacSha256Hex("secret", "1730534400.{}");
        assertEquals(a, b);
    }
}
