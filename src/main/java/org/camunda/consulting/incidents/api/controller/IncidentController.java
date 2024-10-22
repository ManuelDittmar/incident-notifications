package org.camunda.consulting.incidents.api.controller;

import io.camunda.operate.exception.OperateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.consulting.incidents.core.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/incidents")
@Tag(name = "Incidents", description = "Operations related to Incidents in Camunda")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping()
    @Operation(summary = "Get open Incidents", description = "Retrieves summary of open Incidents in Camunda")
    public ResponseEntity<List<?>> getOpenIncidents(
            @RequestParam(name = "includeInstances", required = false, defaultValue = "false") boolean includeInstances
    ) throws OperateException {
        List<?> incidents = incidentService.getIncidentsGroupedByMessage(includeInstances);
        return ResponseEntity.ok(incidents);
    }

}
