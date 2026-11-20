package io.github.aaronfilson.studiomanager.glaze;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Serializes and parses per-line imported oxide chemistry snapshots as JSON. */
final class GlazeChemistryJson {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private GlazeChemistryJson() {}

  static @Nullable String toJson(GlazeBook.@Nullable Chemistry chemistry) {
    if (chemistry == null) {
      return null;
    }
    if (chemistry.materialId() == null && chemistry.analysis().isEmpty() && chemistry.loi() == null) {
      return null;
    }
    Map<String, Object> root = new LinkedHashMap<>();
    if (chemistry.materialId() != null) {
      root.put("materialId", chemistry.materialId());
    }
    if (!chemistry.analysis().isEmpty()) {
      root.put("analysis", chemistry.analysis());
    }
    if (chemistry.loi() != null) {
      root.put("loi", chemistry.loi());
    }
    return MAPPER.writeValueAsString(root);
  }

  static GlazeBook.@Nullable Chemistry fromJson(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      JsonNode node = MAPPER.readTree(raw);
      if (node == null || !node.isObject()) {
        return null;
      }
      String materialId = null;
      JsonNode idNode = node.get("materialId");
      if (idNode != null && idNode.isString() && !idNode.asString().isBlank()) {
        materialId = idNode.asString().strip();
      }
      Map<String, Double> analysis = new LinkedHashMap<>();
      JsonNode analysisNode = node.get("analysis");
      if (analysisNode != null && analysisNode.isObject()) {
        for (var entry : analysisNode.properties()) {
          Double value = parseDouble(entry.getValue());
          if (value != null && Double.isFinite(value) && value >= 0) {
            analysis.put(entry.getKey(), value);
          }
        }
      }
      Double loi = parseDouble(node.get("loi"));
      if (loi != null && (!Double.isFinite(loi) || loi < 0)) {
        loi = null;
      }
      if (materialId == null && analysis.isEmpty() && loi == null) {
        return null;
      }
      return new GlazeBook.Chemistry(materialId, Map.copyOf(analysis), loi);
    } catch (JacksonException e) {
      return null;
    }
  }

  static @Nullable Double parseDouble(@Nullable JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    if (node.isNumber()) {
      return node.doubleValue();
    }
    if (node.isString()) {
      String text = node.asString().strip();
      if (text.isEmpty()) {
        return null;
      }
      try {
        return Double.parseDouble(text);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }
}
