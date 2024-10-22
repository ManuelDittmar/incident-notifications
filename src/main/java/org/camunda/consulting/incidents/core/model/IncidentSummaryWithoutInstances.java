package org.camunda.consulting.incidents.core.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IncidentSummaryWithoutInstances {
    private String message;
    private long count;
    private String url;
}
