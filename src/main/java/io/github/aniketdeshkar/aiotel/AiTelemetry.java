package io.github.aniketdeshkar.aiotel;

import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

public final class AiTelemetry {
  private final ObservationRegistry observationRegistry;
  private final AiTelemetryMetrics metrics;

  public AiTelemetry(ObservationRegistry observationRegistry, AiTelemetryMetrics metrics) {
    this.observationRegistry = Objects.requireNonNull(observationRegistry, "observationRegistry");
    this.metrics = metrics;
  }

  public <T> T observe(AiOperationContext context, Supplier<T> operation) {
    return observe(context, CapturedContent.none(), operation, ignored -> TokenUsage.unavailable());
  }

  public <T> T observe(
      AiOperationContext context,
      CapturedContent capturedContent,
      Supplier<T> operation,
      Function<T, TokenUsage> usageExtractor) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(capturedContent, "capturedContent");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(usageExtractor, "usageExtractor");

    Observation observation = createObservation(context, capturedContent);

    Timer.Sample sample = metrics == null ? null : metrics.start();
    observation.start();
    try (Observation.Scope ignored = observation.openScope()) {
      T result = operation.get();
      TokenUsage usage = Objects.requireNonNull(usageExtractor.apply(result), "token usage");
      recordUsage(observation, context, usage);
      observation.lowCardinalityKeyValue("outcome", "success");
      if (sample != null) {
        metrics.stop(sample, context, "success");
      }
      return result;
    } catch (RuntimeException | Error exception) {
      observation.error(exception);
      observation.lowCardinalityKeyValue("outcome", "error");
      if (sample != null) {
        metrics.stop(sample, context, "error");
      }
      throw exception;
    } finally {
      observation.stop();
    }
  }

  public <T> Flux<T> observeStream(
      AiOperationContext context,
      CapturedContent capturedContent,
      Supplier<Flux<T>> operation,
      Function<T, TokenUsage> usageExtractor) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(capturedContent, "capturedContent");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(usageExtractor, "usageExtractor");
    return Flux.defer(
        () -> {
          Observation observation = createObservation(context, capturedContent).start();
          Timer.Sample sample = metrics == null ? null : metrics.start();
          AtomicReference<T> last = new AtomicReference<>();
          AtomicReference<String> outcome = new AtomicReference<>("success");
          final Flux<T> publisher;
          try (Observation.Scope ignored = observation.openScope()) {
            publisher = operation.get();
          } catch (RuntimeException | Error exception) {
            observation.error(exception);
            observation.lowCardinalityKeyValue("outcome", "error");
            if (sample != null) {
              metrics.stop(sample, context, "error");
            }
            observation.stop();
            throw exception;
          }
          return publisher
              .doOnNext(last::set)
              .doOnError(
                  error -> {
                    outcome.set("error");
                    observation.error(error);
                  })
              .doOnCancel(() -> outcome.set("cancelled"))
              .doFinally(
                  signal -> {
                    if (signal == SignalType.ON_COMPLETE && last.get() != null) {
                      recordUsage(
                          observation,
                          context,
                          Objects.requireNonNull(usageExtractor.apply(last.get()), "token usage"));
                    }
                    observation.lowCardinalityKeyValue("outcome", outcome.get());
                    if (sample != null) {
                      metrics.stop(sample, context, outcome.get());
                    }
                    observation.stop();
                  });
        });
  }

  private Observation createObservation(
      AiOperationContext context, CapturedContent capturedContent) {
    String operationType = context.type().name().toLowerCase(Locale.ROOT);
    Observation observation =
        Observation.createNotStarted("spring.ai." + operationType, observationRegistry)
            .contextualName(operationType + " " + context.name())
            .lowCardinalityKeyValue("gen_ai.operation.name", operationType)
            .lowCardinalityKeyValue("gen_ai.operation.detail", context.name())
            .lowCardinalityKeyValue("gen_ai.provider.name", context.provider())
            .lowCardinalityKeyValue("gen_ai.request.model", context.model())
            .lowCardinalityKeyValue("gen_ai.retry.count", Integer.toString(context.retryCount()))
            .lowCardinalityKeyValue("gen_ai.fallback", Boolean.toString(context.fallback()));
    if (capturedContent.prompt() != null) {
      observation.highCardinalityKeyValue("gen_ai.prompt", capturedContent.prompt());
    }
    if (capturedContent.toolArguments() != null) {
      observation.highCardinalityKeyValue("gen_ai.tool.arguments", capturedContent.toolArguments());
    }
    return observation;
  }

  private void recordUsage(Observation observation, AiOperationContext context, TokenUsage usage) {
    if (usage.inputTokens() != null) {
      observation.highCardinalityKeyValue(
          "gen_ai.usage.input_tokens", usage.inputTokens().toString());
    }
    if (usage.outputTokens() != null) {
      observation.highCardinalityKeyValue(
          "gen_ai.usage.output_tokens", usage.outputTokens().toString());
    }
    if (metrics != null) {
      metrics.tokens(context, usage);
    }
  }
}
