# Local OpenTelemetry Stack

Start the collector and Jaeger:

```bash
docker compose up -d
```

Configure the instrumented Spring Boot application to export OTLP over HTTP:

```yaml
management:
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
  tracing:
    sampling:
      probability: 1.0
```

Run the application and open `http://localhost:16686`. Select the application's service name to inspect the HTTP parent span and its model, retrieval, and tool children.

Stop the stack with `docker compose down`. No data is sent to a hosted service.
