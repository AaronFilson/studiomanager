package io.github.aaronfilson.studiomanager.kiln;

import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Serializes and deserializes downsampled firing curves stored in {@code firing.temperature_curve}. */
final class TemperatureCurveJson {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final TypeReference<List<TemperaturePoint>> LIST_TYPE = new TypeReference<>() {};

  private TemperatureCurveJson() {}

  static @Nullable String write(@Nullable List<TemperaturePoint> curve) {
    if (curve == null || curve.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(curve);
    } catch (JacksonException e) {
      throw new IllegalStateException("Could not serialize firing temperature curve", e);
    }
  }

  static @Nullable List<TemperaturePoint> read(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return List.copyOf(MAPPER.readValue(raw, LIST_TYPE));
    } catch (JacksonException e) {
      throw new IllegalStateException("Stored firing temperature curve is not valid JSON", e);
    }
  }
}
