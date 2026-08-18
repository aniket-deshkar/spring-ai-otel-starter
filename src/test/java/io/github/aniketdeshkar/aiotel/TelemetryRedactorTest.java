package io.github.aniketdeshkar.aiotel;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aniketdeshkar.aiotel.autoconfigure.AiTelemetryProperties.CaptureMode;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TelemetryRedactorTest {
  private final TelemetryRedactor redactor =
      new TelemetryRedactor(
          new ObjectMapper(), Set.of("password", "token"), Set.of("authorization"), 200);

  @Test
  void promptCaptureIsOffUnlessExplicitlyEnabled() {
    assertThat(redactor.capturePrompt("private prompt", false)).isNull();
    assertThat(redactor.capturePrompt("private prompt", true)).isEqualTo("private prompt");
  }

  @Test
  void recursivelyRedactsToolArguments() {
    String captured =
        redactor.captureToolArguments(
            "{\"password\":\"secret\",\"nested\":{\"token\":\"abc\",\"safe\":1}}",
            CaptureMode.REDACTED);
    assertThat(captured)
        .contains("[REDACTED]")
        .contains("\"safe\":1")
        .doesNotContain("secret", "abc");
  }

  @Test
  void malformedRedactedArgumentsNeverExposeInput() {
    assertThat(redactor.captureToolArguments("secret{", CaptureMode.REDACTED))
        .isEqualTo("[UNPARSEABLE]");
  }

  @Test
  void offModeDoesNotParseOrCaptureArguments() {
    assertThat(redactor.captureToolArguments("{\"password\":\"secret\"}", CaptureMode.OFF))
        .isNull();
  }

  @Test
  void redactsConfiguredHeadersCaseInsensitively() {
    assertThat(redactor.redactHeaders(Map.of("Authorization", "Bearer secret", "x-safe", "ok")))
        .containsEntry("Authorization", "[REDACTED]")
        .containsEntry("x-safe", "ok");
  }

  @Test
  void boundsCapturedValues() {
    TelemetryRedactor bounded =
        new TelemetryRedactor(new ObjectMapper(), Set.of("password"), Set.of(), 40);
    assertThat(bounded.capturePrompt("x".repeat(60), true)).hasSize(41).endsWith("…");
  }
}
