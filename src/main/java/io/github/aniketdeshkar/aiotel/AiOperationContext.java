package io.github.aniketdeshkar.aiotel;

import java.util.Objects;

public record AiOperationContext(
    AiOperationType type,
    String name,
    String provider,
    String model,
    int retryCount,
    boolean fallback) {
  public AiOperationContext {
    type = Objects.requireNonNull(type, "type");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    provider = normalize(provider);
    model = normalize(model);
    if (retryCount < 0) {
      throw new IllegalArgumentException("retryCount must be non-negative");
    }
  }

  public static AiOperationContext model(String name, String provider, String model) {
    return new AiOperationContext(AiOperationType.MODEL, name, provider, model, 0, false);
  }

  public static AiOperationContext tool(String name) {
    return new AiOperationContext(AiOperationType.TOOL, name, null, null, 0, false);
  }

  public static AiOperationContext retrieval(String name) {
    return new AiOperationContext(AiOperationType.RETRIEVAL, name, null, null, 0, false);
  }

  public AiOperationContext withRetry(int count) {
    return new AiOperationContext(type, name, provider, model, count, fallback);
  }

  public AiOperationContext asFallback() {
    return new AiOperationContext(type, name, provider, model, retryCount, true);
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
