package io.github.aniketdeshkar.aiotel;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aniketdeshkar.aiotel.autoconfigure.AiTelemetryAutoConfiguration;
import io.github.aniketdeshkar.aiotel.spring.AiTelemetryFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class AiTelemetryAutoConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(AiTelemetryAutoConfiguration.class))
          .withUserConfiguration(ObservabilityConfiguration.class);

  @Test
  void configuresTelemetryWhenObservationRegistryExists() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(AiTelemetry.class);
          assertThat(context).hasSingleBean(TelemetryRedactor.class);
          assertThat(context).hasSingleBean(AiTelemetryFactory.class);
        });
  }

  @Test
  void canBeDisabled() {
    runner
        .withPropertyValues("spring.ai.telemetry.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(AiTelemetry.class));
  }

  @Test
  void rejectsInvalidCaptureConfiguration() {
    runner
        .withPropertyValues("spring.ai.telemetry.tool-arguments-capture=invalid")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void rejectsOutOfBoundsCaptureLength() {
    runner
        .withPropertyValues("spring.ai.telemetry.max-captured-length=0")
        .run(context -> assertThat(context).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  static class ObservabilityConfiguration {
    @Bean
    ObservationRegistry observationRegistry() {
      return ObservationRegistry.create();
    }

    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }
}
