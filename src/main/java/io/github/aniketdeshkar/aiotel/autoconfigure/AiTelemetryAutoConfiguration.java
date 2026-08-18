package io.github.aniketdeshkar.aiotel.autoconfigure;

import io.github.aniketdeshkar.aiotel.AiTelemetry;
import io.github.aniketdeshkar.aiotel.AiTelemetryMetrics;
import io.github.aniketdeshkar.aiotel.TelemetryRedactor;
import io.github.aniketdeshkar.aiotel.spring.AiTelemetryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass({ObservationRegistry.class, io.opentelemetry.api.OpenTelemetry.class})
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnProperty(
    prefix = "spring.ai.telemetry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(AiTelemetryProperties.class)
public class AiTelemetryAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  TelemetryRedactor aiTelemetryRedactor(
      AiTelemetryProperties properties, ObjectProvider<ObjectMapper> objectMapper) {
    return new TelemetryRedactor(
        objectMapper.getIfAvailable(ObjectMapper::new),
        properties.getRedactedFields(),
        properties.getRedactedHeaders(),
        properties.getMaxCapturedLength());
  }

  @Bean
  @ConditionalOnMissingBean
  AiTelemetry aiTelemetry(
      ObservationRegistry observationRegistry, ObjectProvider<MeterRegistry> meterRegistry) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    return new AiTelemetry(
        observationRegistry, registry == null ? null : new AiTelemetryMetrics(registry));
  }

  @Bean
  @ConditionalOnMissingBean
  AiTelemetryFactory aiTelemetryFactory(
      AiTelemetry telemetry, TelemetryRedactor redactor, AiTelemetryProperties properties) {
    return new AiTelemetryFactory(telemetry, redactor, properties);
  }
}
