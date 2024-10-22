package org.camunda.consulting.incidents.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class IncidentSummaryWithInstances {
    private String message;
    private long count;
    private String url;
    private List<Instance> instances;

    @Data
    @Builder
    public static class Instance {
        private long key;
        private long processDefinitionKey;
        private long processInstanceKey;
        private Date creationTime;
    }
}
