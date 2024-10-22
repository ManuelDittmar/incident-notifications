package org.camunda.consulting.incidents.core.service;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.Incident;
import io.camunda.operate.model.SearchResult;
import io.camunda.operate.search.IncidentFilter;
import io.camunda.operate.search.SearchQuery;
import org.camunda.consulting.incidents.core.model.IncidentSummaryWithInstances;
import org.camunda.consulting.incidents.core.model.IncidentSummaryWithoutInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentService.class);
    private static final String PROCESS_ERROR_OVERVIEW_PATH = "/processes?errorMessage=";
    private static final String INCIDENTS_FILTER = "&incidents=true";

    private final CamundaOperateClient operateClient;

    @Value("${incident-alert.operate.base-url}")
    private String operateBaseUrl;


    public IncidentService(CamundaOperateClient operateClient) {
        this.operateClient = operateClient;
    }

    public List<?> getIncidentsGroupedByMessage(boolean includeInstances) throws OperateException {
        List<Incident> incidents = new ArrayList<>();
        fetchIncidents(incidents, null, 0);

        return incidentsToGroupedList(includeInstances, incidents);
    }

    public List<?> incidentsToGroupedList(boolean includeInstances, List<Incident> incidents) {
        Map<String, List<Incident>> groupedByMessage = groupIncidentsByMessage(incidents);

        if (includeInstances) {
            return groupedByMessage.entrySet().stream()
                    .map(entry -> buildIncidentSummaryWithInstances(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        } else {
            return groupedByMessage.entrySet().stream()
                    .map(entry -> buildIncidentSummaryWithoutInstances(entry.getKey(), entry.getValue().size()))
                    .collect(Collectors.toList());
        }
    }

    public Map<String, List<Incident>> groupIncidentsByMessage(List<Incident> incidents) {
        return incidents.stream()
                .collect(Collectors.groupingBy(Incident::getMessage));
    }

    private String buildIncidentUrl(String errorMessage) {
        return operateBaseUrl + PROCESS_ERROR_OVERVIEW_PATH + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8) + INCIDENTS_FILTER;
    }

    private IncidentSummaryWithInstances buildIncidentSummaryWithInstances(String message, List<Incident> incidents) {
        return IncidentSummaryWithInstances.builder()
                .message(message)
                .count(incidents.size())
                .url(buildIncidentUrl(message))
                .instances(incidents.stream()
                        .map(incident -> IncidentSummaryWithInstances.Instance.builder()
                                .key(incident.getKey())
                                .processDefinitionKey(incident.getProcessDefinitionKey())
                                .processInstanceKey(incident.getProcessInstanceKey())
                                .creationTime(incident.getCreationTime())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private IncidentSummaryWithoutInstances buildIncidentSummaryWithoutInstances(String message, long count) {
        return IncidentSummaryWithoutInstances.builder()
                .message(message)
                .count(count)
                .url(buildIncidentUrl(message))
                .build();
    }


    public List<Incident> fetchIncidents(List<Incident> incidents, List<Object> sortValues, int cumulativeFetchedCount) throws OperateException {
        SearchQuery searchQuery = new SearchQuery();
        IncidentFilter incidentFilter = new IncidentFilter();
        incidentFilter.setState("ACTIVE");
        searchQuery.setFilter(incidentFilter);
        SearchResult<Incident> searchResult;

        if (sortValues != null) {
            searchQuery.setSearchAfter(sortValues);
        }

        try {
            searchResult = operateClient.searchIncidentResults(searchQuery);
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while fetching Incidents", e);
        }

        if (searchResult.getTotal() == 0) {
            LOGGER.info("No incidents found");
            return null;
        }

        cumulativeFetchedCount += searchResult.getItems().size();

        incidents.addAll(searchResult.getItems());

        if (searchResult.getTotal() > cumulativeFetchedCount) {
            LOGGER.debug("Total process instances: {}, Fetched process instances: {}", searchResult.getTotal(), cumulativeFetchedCount);
            fetchIncidents(incidents, searchResult.getSortValues(), cumulativeFetchedCount);
        }

        return incidents;
    }
}
