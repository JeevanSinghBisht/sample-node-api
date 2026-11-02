package com.platform.cursor.bridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public class BridgeProperties {

    private CursorApi cursor = new CursorApi();
    private Registry registry = new Registry();
    private InternalApi internal = new InternalApi();
    private Webhooks webhooks = new Webhooks();
    private String defaultModelId = "";

    public CursorApi getCursor() {
        return cursor;
    }

    public void setCursor(CursorApi cursor) {
        this.cursor = cursor;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public InternalApi getInternal() {
        return internal;
    }

    public void setInternal(InternalApi internal) {
        this.internal = internal;
    }

    public Webhooks getWebhooks() {
        return webhooks;
    }

    public void setWebhooks(Webhooks webhooks) {
        this.webhooks = webhooks;
    }

    public String getDefaultModelId() {
        return defaultModelId;
    }

    public void setDefaultModelId(String defaultModelId) {
        this.defaultModelId = defaultModelId;
    }

    public static class CursorApi {
        private String baseUrl = "https://api.cursor.com";
        private String apiKey = "";
        private boolean mockEnabled = false;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isMockEnabled() {
            return mockEnabled;
        }

        public void setMockEnabled(boolean mockEnabled) {
            this.mockEnabled = mockEnabled;
        }
    }

    public static class Registry {
        private String baseUrl = "http://localhost:8081";
        private String hmacSecret = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }
    }

    public static class InternalApi {
        private boolean hmacEnabled = true;
        private String hmacSecret = "";
        private long maxTimestampSkewSeconds = 300;

        public boolean isHmacEnabled() {
            return hmacEnabled;
        }

        public void setHmacEnabled(boolean hmacEnabled) {
            this.hmacEnabled = hmacEnabled;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public long getMaxTimestampSkewSeconds() {
            return maxTimestampSkewSeconds;
        }

        public void setMaxTimestampSkewSeconds(long maxTimestampSkewSeconds) {
            this.maxTimestampSkewSeconds = maxTimestampSkewSeconds;
        }
    }

    public static class Webhooks {
        private String completionSecret = "";
        private String n8nCompletionUrl = "";

        public String getCompletionSecret() {
            return completionSecret;
        }

        public void setCompletionSecret(String completionSecret) {
            this.completionSecret = completionSecret;
        }

        public String getN8nCompletionUrl() {
            return n8nCompletionUrl;
        }

        public void setN8nCompletionUrl(String n8nCompletionUrl) {
            this.n8nCompletionUrl = n8nCompletionUrl;
        }
    }
}
