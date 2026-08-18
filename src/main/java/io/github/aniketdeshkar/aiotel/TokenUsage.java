package io.github.aniketdeshkar.aiotel;

public record TokenUsage(Integer inputTokens, Integer outputTokens) {
  public TokenUsage {
    if (inputTokens != null && inputTokens < 0) {
      throw new IllegalArgumentException("inputTokens must be non-negative");
    }
    if (outputTokens != null && outputTokens < 0) {
      throw new IllegalArgumentException("outputTokens must be non-negative");
    }
  }

  public static TokenUsage unavailable() {
    return new TokenUsage(null, null);
  }
}
