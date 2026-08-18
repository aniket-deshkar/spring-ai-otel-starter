package io.github.aniketdeshkar.aiotel;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aniketdeshkar.aiotel.autoconfigure.AiTelemetryProperties.CaptureMode;
import io.github.aniketdeshkar.aiotel.spring.ObservedChatModel;
import io.github.aniketdeshkar.aiotel.spring.ObservedRetriever;
import io.github.aniketdeshkar.aiotel.spring.ObservedToolCallback;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

class SpringAiWrappersTest {
  private final ObservationRegistry registry = ObservationRegistry.create();
  private final List<Observation.Context> stopped = new ArrayList<>();
  private final AiTelemetry telemetry = new AiTelemetry(registry, null);
  private final TelemetryRedactor redactor =
      new TelemetryRedactor(new ObjectMapper(), Set.of("password"), Set.of(), 200);

  @BeforeEach
  void collect() {
    registry
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
  void toolWrapperRedactsArgumentsAndInvokesDelegate() {
    AtomicInteger calls = new AtomicInteger();
    ToolCallback observed =
        new ObservedToolCallback(tool(calls), telemetry, redactor, CaptureMode.REDACTED);

    assertThat(observed.call("{\"password\":\"secret\",\"city\":\"Pune\"}")).isEqualTo("ok");

    assertThat(calls).hasValue(1);
    String captured =
        stopped.getFirst().getHighCardinalityKeyValue("gen_ai.tool.arguments").getValue();
    assertThat(captured).contains("[REDACTED]", "Pune").doesNotContain("secret");
  }

  @Test
  void chatWrapperRecordsProviderUsageWithoutPromptByDefault() {
    ObservedChatModel observed =
        new ObservedChatModel(chatModel(), telemetry, redactor, "provider", "model-a", false);

    observed.call(new Prompt("private prompt"));

    Observation.Context context = stopped.getFirst();
    assertThat(context.getHighCardinalityKeyValue("gen_ai.prompt")).isNull();
    assertThat(context.getHighCardinalityKeyValue("gen_ai.usage.input_tokens").getValue())
        .isEqualTo("7");
  }

  @Test
  void streamingChatStopsObservationOnCompletion() {
    ObservedChatModel observed =
        new ObservedChatModel(chatModel(), telemetry, redactor, "provider", "model-a", false);

    assertThat(observed.stream(new Prompt("prompt")).collectList().block()).hasSize(1);

    Observation.Context context = stopped.getFirst();
    assertThat(context.getName()).isEqualTo("spring.ai.model");
    assertThat(context.getLowCardinalityKeyValue("gen_ai.operation.detail").getValue())
        .isEqualTo("chat.stream");
    assertThat(context.getHighCardinalityKeyValue("gen_ai.usage.output_tokens").getValue())
        .isEqualTo("3");
  }

  @Test
  void retrievalWrapperCreatesObservation() {
    ObservedRetriever<String> retriever =
        new ObservedRetriever<>("documents", query -> List.of("one"), telemetry);
    assertThat(retriever.apply("query")).containsExactly("one");
    assertThat(stopped.getFirst().getName()).isEqualTo("spring.ai.retrieval");
  }

  private static ToolCallback tool(AtomicInteger calls) {
    ToolDefinition definition =
        ToolDefinition.builder()
            .name("weather")
            .description("Weather")
            .inputSchema("{\"type\":\"object\"}")
            .build();
    return new ToolCallback() {
      @Override
      public ToolDefinition getToolDefinition() {
        return definition;
      }

      @Override
      public String call(String input) {
        calls.incrementAndGet();
        return "ok";
      }
    };
  }

  private static ChatModel chatModel() {
    ChatResponse response =
        ChatResponse.builder()
            .generations(List.of())
            .metadata(ChatResponseMetadata.builder().usage(usage()).build())
            .build();
    return new ChatModel() {
      @Override
      public ChatResponse call(Prompt prompt) {
        return response;
      }

      @Override
      public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(response);
      }
    };
  }

  private static Usage usage() {
    return new Usage() {
      @Override
      public Integer getPromptTokens() {
        return 7;
      }

      @Override
      public Integer getCompletionTokens() {
        return 3;
      }

      @Override
      public Object getNativeUsage() {
        return null;
      }
    };
  }
}
