package org.camunda.consulting.incidents;

import org.camunda.consulting.incidents.core.config.IncidentAlertProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(IncidentAlertProperties.class)
public class IncidentAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentAlertsApplication.class, args);
    }

}
