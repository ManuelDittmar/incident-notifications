package org.camunda.consulting.incidents.core.alerts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.model.Incident;
import org.camunda.consulting.incidents.core.config.IncidentAlertProperties;
import org.camunda.consulting.incidents.core.model.IncidentSummaryWithoutInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.List;

@Component
public class IncidentPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentPublisher.class);
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().build();

    public IncidentPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publishIndividualIncident(Incident incident, IncidentAlertProperties.Subscription subscription) {
        LOGGER.debug("Publishing IndividualIncidentEvent for subscription {}", subscription);
        String message = buildMessage(incident);
        switch (subscription.getIntegration().getType()) {
            case WEBHOOK:
                publishToWebhook(subscription.getIntegration().getWebhook(), message);
                break;
            case SNS:
                publishToSns(subscription.getIntegration().getSns(), message);
                break;
            default:
                LOGGER.error("Integration type not supported");
        }
    }

    public void publishSummaryIncident(List<IncidentSummaryWithoutInstances> incidents, IncidentAlertProperties.Subscription subscription) throws JsonProcessingException {
        String message = buildMessage(incidents);
        switch (subscription.getIntegration().getType()) {
            case WEBHOOK:
                publishToWebhook(subscription.getIntegration().getWebhook(), message);
                break;
            case SNS:
                publishToSns(subscription.getIntegration().getSns(), message);
                break;
            default:
                LOGGER.error("Integration type not supported");
        }
    }

    private void publishToWebhook(IncidentAlertProperties.WebhookIntegration webhook, String message) {
        LOGGER.debug("Publishing incident to webhook {}", webhook.getUrl());
        webClient.post()
                .uri(webhook.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    LOGGER.error("Failed to publish to webhook: {}", webhook.getUrl());
                    return clientResponse.createException();
                })
                .bodyToMono(Void.class)
                .doOnSuccess(response -> LOGGER.debug("Successfully published incident to webhook {}", webhook.getUrl()))
                .doOnError(error -> LOGGER.error("Error occurred while publishing to webhook: {}", error.getMessage()))
                .subscribe();
    }

    private void publishToSns(IncidentAlertProperties.SnsIntegration snsIntegration, String message) {
        SnsClient snsClient = SnsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(snsIntegration.getAccessKey(), snsIntegration.getSecretKey())))
                .region(Region.of(snsIntegration.getRegion()))
                .build();

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(snsIntegration.getTopicArn())
                .message(message)
                .build();

        PublishResponse result = snsClient.publish(publishRequest);
        LOGGER.debug("Published message to SNS, MessageId: {}", result.messageId());

        snsClient.close();
    }

    private String buildMessage(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize incident to JSON", e);
            throw new RuntimeException("Failed to serialize incident to JSON", e);
        }
    }
}
