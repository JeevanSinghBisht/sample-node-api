package com.platform.workflow.registry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {

    private InternalApi internal = new InternalApi();
    private Github github = new Github();
    private N8n n8n = new N8n();
    private Dashboard dashboard = new Dashboard();

    public InternalApi getInternal() {
        return internal;
    }

    public void setInternal(InternalApi internal) {
        this.internal = internal;
    }

    public Github getGithub() {
        return github;
    }

    public void setGithub(Github github) {
        this.github = github;
    }

    public N8n getN8n() {
        return n8n;
    }

    public void setN8n(N8n n8n) {
        this.n8n = n8n;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
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

    public static class Github {
        private String webhookSecret = "";
        private String readyLabel = "agent:ready";

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getReadyLabel() {
            return readyLabel;
        }

        public void setReadyLabel(String readyLabel) {
            this.readyLabel = readyLabel;
        }
    }

    public static class N8n {
        private String taskStateWebhookUrl = "";

        public String getTaskStateWebhookUrl() {
            return taskStateWebhookUrl;
        }

        public void setTaskStateWebhookUrl(String taskStateWebhookUrl) {
            this.taskStateWebhookUrl = taskStateWebhookUrl;
        }
    }

    public static class Dashboard {
        private String allowedOrigins = "http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
