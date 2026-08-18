package io.github.aniketdeshkar.aiotel;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.Objects;

public final class AiTelemetryMetrics {
  private final MeterRegistry registry;

  public AiTelemetryMetrics(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  Timer.Sample start() {
    return Timer.start(registry);
  }

  void stop(Timer.Sample sample, AiOperationContext context, String outcome) {
    sample.stop(
        Timer.builder("spring.ai.telemetry.operation")
            .tag("operation", context.type().name().toLowerCase(Locale.ROOT))
            .tag("name", context.name())
            .tag("outcome", outcome)
            .register(registry));
  }

  void tokens(AiOperationContext context, TokenUsage usage) {
    if (usage.inputTokens() != null) {
      registry
          .counter("spring.ai.telemetry.tokens", "operation", context.name(), "type", "input")
          .increment(usage.inputTokens());
    }
    if (usage.outputTokens() != null) {
      registry
          .counter("spring.ai.telemetry.tokens", "operation", context.name(), "type", "output")
          .increment(usage.outputTokens());
    }
  }
}
