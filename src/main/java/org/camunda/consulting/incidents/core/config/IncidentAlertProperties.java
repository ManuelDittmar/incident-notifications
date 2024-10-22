package org.camunda.consulting.incidents.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "incident-alert")
public class IncidentAlertProperties {

    private OperateConfig operate;
    private boolean enabled;
    private String cronExpression;
    private List<Subscription> subscriptions;

    @Data
    public static class OperateConfig {
        private String baseUrl;
    }

    @Data
    public static class Subscription {
        private SubscriptionType type;
        private Integration integration;
    }

    @Data
    public static class Integration {
        private IntegrationType type;
        private WebhookIntegration webhook;
        private SnsIntegration sns;
    }

    @Data
    public static class WebhookIntegration {
        private String url;
    }

    @Data
    public static class SnsIntegration {
        private String topicArn;
        private String accessKey;
        private String secretKey;
        private String region;
    }
}
