package io.github.aniketdeshkar.aiotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiTelemetryTest {
  private final ObservationRegistry observationRegistry = ObservationRegistry.create();
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final List<Observation.Context> stopped = new ArrayList<>();
  private final AiTelemetry telemetry =
      new AiTelemetry(observationRegistry, new AiTelemetryMetrics(meterRegistry));

  @BeforeEach
  void collectObservations() {
    observationRegistry
        .observationConfig()
        .observationHandler(
            new ObservationHandler<>() {
              @Override
              public void onStop(Observation.Context context) {
                stopped.add(context);
              }

              @Override
              public boolean supportsContext(Observation.Context context) {
                return true;
              }
            });
  }

  @Test
  void preservesHttpParentAcrossAgentModelAndToolOperations() {
    Observation http = Observation.start("http.server.requests", observationRegistry);
    try (Observation.Scope ignored = http.openScope()) {
      telemetry.observe(
          new AiOperationContext(AiOperationType.WORKFLOW, "agent", null, null, 0, false),
          () -> {
            telemetry.observe(
                AiOperationContext.model("chat", "openai", "model-a"), () -> "model response");
            telemetry.observe(AiOperationContext.tool("weather"), () -> "28 C");
            return "done";
          });
    } finally {
      http.stop();
    }

    Observation.Context workflow = context("spring.ai.workflow");
    Observation.Context model = context("spring.ai.model");
    Observation.Context tool = context("spring.ai.tool");
    assertThat(workflow.getParentObservation().getContextView().getName())
        .isEqualTo("http.server.requests");
    assertThat(model.getParentObservation().getContextView().getName())
        .isEqualTo("spring.ai.workflow");
    assertThat(tool.getParentObservation().getContextView().getName())
        .isEqualTo("spring.ai.workflow");
  }

  @Test
  void recordsProvidedTokensAndRetryFallbackAttributes() {
    AiOperationContext context =
        AiOperationContext.model("chat", "provider", "model").withRetry(2).asFallback();
    telemetry.observe(
        context, CapturedContent.none(), () -> "response", ignored -> new TokenUsage(12, 4));

    Observation.Context observed = context("spring.ai.model");
    assertThat(observed.getLowCardinalityKeyValue("gen_ai.retry.count").getValue()).isEqualTo("2");
    assertThat(observed.getLowCardinalityKeyValue("gen_ai.fallback").getValue()).isEqualTo("true");
    assertThat(observed.getHighCardinalityKeyValue("gen_ai.usage.input_tokens").getValue())
        .isEqualTo("12");
    assertThat(
            meterRegistry
                .get("spring.ai.telemetry.tokens")
                .tags("operation", "chat", "type", "input")
                .counter()
                .count())
        .isEqualTo(12);
  }

  @Test
  void doesNotInventUnavailableTokenAttributes() {
    telemetry.observe(AiOperationContext.model("chat", "provider", "model"), () -> "response");
    assertThat(context("spring.ai.model").getHighCardinalityKeyValue("gen_ai.usage.input_tokens"))
        .isNull();
  }

  @Test
  void recordsErrorsAndRethrows() {
    assertThatThrownBy(
            () ->
                telemetry.observe(
                    AiOperationContext.tool("broken"),
                    () -> {
                      throw new IllegalStateException("failure");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(context("spring.ai.tool").getError()).isInstanceOf(IllegalStateException.class);
  }

  private Observation.Context context(String name) {
    return stopped.stream().filter(value -> name.equals(value.getName())).findFirst().orElseThrow();
  }
}
