package io.github.aniketdeshkar.aiotel;

import io.github.aniketdeshkar.aiotel.autoconfigure.AiTelemetryProperties.CaptureMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class TelemetryRedactor {
  private static final String REDACTED = "[REDACTED]";
  private static final String UNPARSEABLE = "[UNPARSEABLE]";

  private final ObjectMapper objectMapper;
  private final Set<String> fields;
  private final Set<String> headers;
  private final int maximumLength;

  public TelemetryRedactor(
      ObjectMapper objectMapper, Set<String> fields, Set<String> headers, int maximumLength) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.fields = normalize(fields);
    this.headers = normalize(headers);
    if (maximumLength < 1) {
      throw new IllegalArgumentException("maximumLength must be positive");
    }
    this.maximumLength = maximumLength;
  }

  public String capturePrompt(String prompt, boolean enabled) {
    return enabled ? truncate(prompt) : null;
  }

  public String captureToolArguments(String arguments, CaptureMode mode) {
    return switch (mode) {
      case OFF -> null;
      case FULL -> truncate(arguments);
      case REDACTED -> redactJson(arguments);
    };
  }

  public Map<String, String> redactHeaders(Map<String, String> values) {
    Map<String, String> result = new LinkedHashMap<>();
    values.forEach(
        (key, value) ->
            result.put(
                key, headers.contains(key.toLowerCase(Locale.ROOT)) ? REDACTED : truncate(value)));
    return Map.copyOf(result);
  }

  private String redactJson(String value) {
    try {
      JsonNode root = objectMapper.readTree(value);
      redactNode(root);
      return truncate(objectMapper.writeValueAsString(root));
    } catch (RuntimeException exception) {
      return UNPARSEABLE;
    }
  }

  private void redactNode(JsonNode node) {
    if (node instanceof ObjectNode object) {
      object
          .properties()
          .forEach(
              entry -> {
                if (fields.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                  object.put(entry.getKey(), REDACTED);
                } else {
                  redactNode(entry.getValue());
                }
              });
    } else if (node instanceof ArrayNode array) {
      array.forEach(this::redactNode);
    }
  }

  private String truncate(String value) {
    if (value == null || value.length() <= maximumLength) {
      return value;
    }
    return value.substring(0, maximumLength) + "…";
  }

  private static Set<String> normalize(Set<String> values) {
    return values.stream()
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }
}
