package io.github.aniketdeshkar.aiotel.spring;

import io.github.aniketdeshkar.aiotel.AiOperationContext;
import io.github.aniketdeshkar.aiotel.AiTelemetry;
import io.github.aniketdeshkar.aiotel.CapturedContent;
import io.github.aniketdeshkar.aiotel.TelemetryRedactor;
import io.github.aniketdeshkar.aiotel.TokenUsage;
import java.util.Objects;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public final class ObservedChatModel implements ChatModel {
  private final ChatModel delegate;
  private final AiTelemetry telemetry;
  private final TelemetryRedactor redactor;
  private final String provider;
  private final String model;
  private final boolean promptCapture;

  public ObservedChatModel(
      ChatModel delegate,
      AiTelemetry telemetry,
      TelemetryRedactor redactor,
      String provider,
      String model,
      boolean promptCapture) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    this.redactor = Objects.requireNonNull(redactor, "redactor");
    this.provider = provider;
    this.model = model;
    this.promptCapture = promptCapture;
  }

  @Override
  public ChatResponse call(Prompt prompt) {
    String capturedPrompt = redactor.capturePrompt(prompt.getContents(), promptCapture);
    return telemetry.observe(
        AiOperationContext.model("chat", provider, model),
        CapturedContent.prompt(capturedPrompt),
        () -> delegate.call(prompt),
        ObservedChatModel::usage);
  }

  @Override
  public Flux<ChatResponse> stream(Prompt prompt) {
    String capturedPrompt = redactor.capturePrompt(prompt.getContents(), promptCapture);
    return telemetry.observeStream(
        AiOperationContext.model("chat.stream", provider, model),
        CapturedContent.prompt(capturedPrompt),
        () -> delegate.stream(prompt),
        ObservedChatModel::usage);
  }

  @Override
  public ChatOptions getOptions() {
    return delegate.getOptions();
  }

  private static TokenUsage usage(ChatResponse response) {
    if (response == null || response.getMetadata() == null) {
      return TokenUsage.unavailable();
    }
    Usage usage = response.getMetadata().getUsage();
    return usage == null
        ? TokenUsage.unavailable()
        : new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens());
  }
}
