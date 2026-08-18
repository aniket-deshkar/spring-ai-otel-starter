package io.github.aniketdeshkar.aiotel.autoconfigure;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.ai.telemetry")
public class AiTelemetryProperties {
  public enum CaptureMode {
    OFF,
    REDACTED,
    FULL
  }

  private boolean enabled = true;
  private boolean promptCapture = false;
  private CaptureMode toolArgumentsCapture = CaptureMode.OFF;
  private int maxCapturedLength = 2048;
  private Set<String> redactedFields =
      new LinkedHashSet<>(
          Set.of("authorization", "cookie", "api_key", "password", "secret", "token"));
  private Set<String> redactedHeaders =
      new LinkedHashSet<>(Set.of("authorization", "cookie", "set-cookie", "x-api-key"));

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isPromptCapture() {
    return promptCapture;
  }

  public void setPromptCapture(boolean promptCapture) {
    this.promptCapture = promptCapture;
  }

  public CaptureMode getToolArgumentsCapture() {
    return toolArgumentsCapture;
  }

  public void setToolArgumentsCapture(CaptureMode toolArgumentsCapture) {
    if (toolArgumentsCapture == null) {
      throw new IllegalArgumentException("toolArgumentsCapture must not be null");
    }
    this.toolArgumentsCapture = toolArgumentsCapture;
  }

  public int getMaxCapturedLength() {
    return maxCapturedLength;
  }

  public void setMaxCapturedLength(int maxCapturedLength) {
    if (maxCapturedLength < 1 || maxCapturedLength > 32768) {
      throw new IllegalArgumentException("maxCapturedLength must be between 1 and 32768");
    }
    this.maxCapturedLength = maxCapturedLength;
  }

  public Set<String> getRedactedFields() {
    return redactedFields;
  }

  public void setRedactedFields(Set<String> redactedFields) {
    this.redactedFields = new LinkedHashSet<>(redactedFields);
  }

  public Set<String> getRedactedHeaders() {
    return redactedHeaders;
  }

  public void setRedactedHeaders(Set<String> redactedHeaders) {
    this.redactedHeaders = new LinkedHashSet<>(redactedHeaders);
  }
}
