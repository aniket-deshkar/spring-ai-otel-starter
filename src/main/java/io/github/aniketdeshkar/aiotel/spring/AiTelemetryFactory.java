package io.github.aniketdeshkar.aiotel.spring;

import io.github.aniketdeshkar.aiotel.AiTelemetry;
import io.github.aniketdeshkar.aiotel.TelemetryRedactor;
import io.github.aniketdeshkar.aiotel.autoconfigure.AiTelemetryProperties;
import java.util.Objects;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

public final class AiTelemetryFactory {
  private final AiTelemetry telemetry;
  private final TelemetryRedactor redactor;
  private final AiTelemetryProperties properties;

  public AiTelemetryFactory(
      AiTelemetry telemetry, TelemetryRedactor redactor, AiTelemetryProperties properties) {
    this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    this.redactor = Objects.requireNonNull(redactor, "redactor");
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  public ChatModel observeChatModel(ChatModel model, String provider, String modelName) {
    return new ObservedChatModel(
        model, telemetry, redactor, provider, modelName, properties.isPromptCapture());
  }

  public ToolCallback observeTool(ToolCallback callback) {
    return new ObservedToolCallback(
        callback, telemetry, redactor, properties.getToolArgumentsCapture());
  }

  public <T> ObservedRetriever<T> observeRetriever(
      String name, java.util.function.Function<String, java.util.List<T>> retriever) {
    return new ObservedRetriever<>(name, retriever, telemetry);
  }
}
