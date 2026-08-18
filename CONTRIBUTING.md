# Contributing

Create a focused branch and run `./mvnw verify`. Add deterministic tests for span parenting, success and error termination, token absence, and capture safety when changing instrumentation.

Keep metric tags bounded and align attributes with established OpenTelemetry and Micrometer conventions. Never commit credentials, captured prompts, trace exports, build output, or IDE metadata.
