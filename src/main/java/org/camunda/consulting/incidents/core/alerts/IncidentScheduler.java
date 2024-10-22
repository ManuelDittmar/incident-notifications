package org.camunda.consulting.incidents.core.alerts;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.Incident;
import org.camunda.consulting.incidents.core.config.IncidentAlertProperties;
import org.camunda.consulting.incidents.core.config.SubscriptionType;
import org.camunda.consulting.incidents.core.model.IncidentSummaryWithoutInstances;
import org.camunda.consulting.incidents.core.service.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ConditionalOnProperty(value = "incident-alert.enabled", havingValue = "true")
@Component
public class IncidentScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentScheduler.class);

    private final IncidentService incidentService;
    private final IncidentPublisher incidentPublisher;
    private final List<IncidentAlertProperties.Subscription> summarySubscriptions;
    private final List<IncidentAlertProperties.Subscription> individualSubscriptions;

    public IncidentScheduler(IncidentService incidentService, IncidentPublisher incidentPublisher, IncidentAlertProperties incidentAlertProperties) {
        this.incidentService = incidentService;
        this.incidentPublisher = incidentPublisher;
        this.summarySubscriptions = incidentAlertProperties.getSubscriptions().stream()
                .filter(subscription -> subscription.getType() == SubscriptionType.SUMMARY)
                .collect(Collectors.toList());
        this.individualSubscriptions = incidentAlertProperties.getSubscriptions().stream()
                .filter(subscription -> subscription.getType() == SubscriptionType.INDIVIDUAL)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "${incident-alert.cron-expression}")
    public void scheduleIncidentFetching() throws OperateException, JsonProcessingException {
        List<Incident> incidents = incidentService.fetchIncidents(new ArrayList<>(), null, 0);
        for (IncidentAlertProperties.Subscription subscription : individualSubscriptions) {
            for (Incident incident : incidents) {
                incidentPublisher.publishIndividualIncident(incident, subscription);
            }
        }

        if (!incidents.isEmpty()) {
            for (IncidentAlertProperties.Subscription subscription : summarySubscriptions) {
                incidentPublisher.publishSummaryIncident((List<IncidentSummaryWithoutInstances>) incidentService.incidentsToGroupedList(false, incidents), subscription);
            }
        }
    }
}
