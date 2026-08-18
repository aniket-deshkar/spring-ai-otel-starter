package io.github.aniketdeshkar.aiotel.spring;

import io.github.aniketdeshkar.aiotel.AiOperationContext;
import io.github.aniketdeshkar.aiotel.AiTelemetry;
import io.github.aniketdeshkar.aiotel.CapturedContent;
import io.github.aniketdeshkar.aiotel.TelemetryRedactor;
import io.github.aniketdeshkar.aiotel.TokenUsage;
import io.github.aniketdeshkar.aiotel.autoconfigure.AiTelemetryProperties.CaptureMode;
import java.util.Objects;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public final class ObservedToolCallback implements ToolCallback {
  private final ToolCallback delegate;
  private final AiTelemetry telemetry;
  private final TelemetryRedactor redactor;
  private final CaptureMode captureMode;

  public ObservedToolCallback(
      ToolCallback delegate,
      AiTelemetry telemetry,
      TelemetryRedactor redactor,
      CaptureMode captureMode) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    this.redactor = Objects.requireNonNull(redactor, "redactor");
    this.captureMode = Objects.requireNonNull(captureMode, "captureMode");
  }

  @Override
  public ToolDefinition getToolDefinition() {
    return delegate.getToolDefinition();
  }

  @Override
  public ToolMetadata getToolMetadata() {
    return delegate.getToolMetadata();
  }

  @Override
  public String call(String toolInput) {
    return observe(toolInput, () -> delegate.call(toolInput));
  }

  @Override
  public String call(String toolInput, ToolContext toolContext) {
    return observe(toolInput, () -> delegate.call(toolInput, toolContext));
  }

  private String observe(String toolInput, java.util.function.Supplier<String> operation) {
    String captured = redactor.captureToolArguments(toolInput, captureMode);
    return telemetry.observe(
        AiOperationContext.tool(getToolDefinition().name()),
        CapturedContent.toolArguments(captured),
        operation,
        ignored -> TokenUsage.unavailable());
  }
}
