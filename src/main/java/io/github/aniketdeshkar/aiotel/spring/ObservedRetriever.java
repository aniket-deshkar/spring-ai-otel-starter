package io.github.aniketdeshkar.aiotel.spring;

import io.github.aniketdeshkar.aiotel.AiOperationContext;
import io.github.aniketdeshkar.aiotel.AiTelemetry;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ObservedRetriever<T> implements Function<String, List<T>> {
  private final String name;
  private final Function<String, List<T>> delegate;
  private final AiTelemetry telemetry;

  public ObservedRetriever(String name, Function<String, List<T>> delegate, AiTelemetry telemetry) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.name = name;
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
  }

  @Override
  public List<T> apply(String query) {
    return telemetry.observe(AiOperationContext.retrieval(name), () -> delegate.apply(query));
  }
}
