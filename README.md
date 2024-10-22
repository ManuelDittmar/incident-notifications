# Incident Notification Service

This service is responsible for handling incident notifications. It periodically checks for incidents in Camunda by querying the Operate API.

## Limitations

- FIFO Queues for SNS are NOT supported
- Queries for all open Incidents
- Webhook cannot be secured
- No retries for failed notifications
- No idempotency for notifications
## Configuration

###  Incident Alerts
```
incident-alert:
  operate:
    base-url: https://example.com/operate
  enabled: true
  cron-expression: "*/30 * * * * *"
  subscriptions:
    - type: SUMMARY
      integration:
        type: WEBHOOK
        webhook:
          url: https://your-webhook-url
    - type: INDIVIDUAL
      integration:
        type: SNS
        sns:
          topic-arn: arn:aws:sns:region:account-id:topic-name
          access-key: <your-access-key>
          secret-key: <your-secret-key>
          region: eu-central-1
logging:
  level:
    org.camunda.consulting.incidents: DEBUG
    org.springframework.expression: DEBUG

```
- **Operate Base URL**: Set the Camunda Operate URL.
- **Enabled**: Turns the service on or off.
- **Cron Expression**: Defines how often incidents are fetched.
- **Subscriptions**: Supports multiple types (WEBHOOK, SNS).
- **Type**: Choose between SUMMARY and INDIVIDUAL. Individual creates a message for each incident. Summary creates a summary of all incidents grouped by message.
- **Webhook**: Specify a webhook URL to receive incident summaries.
- **SNS**: Use AWS SNS with access keys and topic ARN for notifications.
- **Logging**: Set debug level for detailed logs.

### Operate Client

Please refer to
the [Operate Spring Boot Starter](https://github.com/camunda-community-hub/camunda-operate-client-java/tree/main)
documentation for configuration options.


## OpenAPI Specification

The OpenAPI spec is available at the following URL when the application is running: ``/swagger-ui/index.html`` or
``/v3/api-docs``

## Docker Image

### Step 1: Build the Spring Boot Application JAR

First, ensure that you have built your Spring Boot application by running the following command in the root of your
Spring Boot project directory:

```bash
mvn clean package
```

This will generate a `.jar` file in the `target` directory of your project.

### Step 2: Build the Docker Image

Run the following command to build the Docker image. Be sure to run this command in the same directory as the
`Dockerfile`:

```bash
docker build -t incident-notification .
```

### Step 3: Run the Docker Container

Once the image is built, you can run the Docker container with the following command:

```bash
docker run -p 8080:8080 incident-notification
```

This will start the Spring Boot application inside a Docker container and map port `8080` on your local machine to port
`8080` inside the container.