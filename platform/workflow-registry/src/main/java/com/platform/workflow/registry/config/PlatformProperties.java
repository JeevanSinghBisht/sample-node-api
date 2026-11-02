package com.platform.workflow.registry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {

    private InternalApi internal = new InternalApi();

    public InternalApi getInternal() {
        return internal;
    }

    public void setInternal(InternalApi internal) {
        this.internal = internal;
    }

    public static class InternalApi {
        private String hmacSecret = "";
        private boolean hmacEnabled = true;
        private long maxTimestampSkewSeconds = 300;

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public boolean isHmacEnabled() {
            return hmacEnabled;
        }

        public void setHmacEnabled(boolean hmacEnabled) {
            this.hmacEnabled = hmacEnabled;
        }

        public long getMaxTimestampSkewSeconds() {
            return maxTimestampSkewSeconds;
        }

        public void setMaxTimestampSkewSeconds(long maxTimestampSkewSeconds) {
            this.maxTimestampSkewSeconds = maxTimestampSkewSeconds;
        }
    }
}
