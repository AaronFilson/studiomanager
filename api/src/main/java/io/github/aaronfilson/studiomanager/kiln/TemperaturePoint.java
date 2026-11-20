package io.github.aaronfilson.studiomanager.kiln;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One downsampled point on a firing temperature curve.
 *
 * @param elapsedMinutes minutes elapsed since the first sample in the firing
 * @param temp primary temperature (average across zones for multi-zone kilns)
 * @param setpoint controller target temperature, when logged
 * @param zoneTemps individual thermocouple readings (for example TC1, TC2, TC3), when logged
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemperaturePoint(
    double elapsedMinutes,
    double temp,
    @Nullable Double setpoint,
    @Nullable List<Double> zoneTemps) {

  public TemperaturePoint {
    zoneTemps = zoneTemps == null ? null : List.copyOf(zoneTemps);
  }
}
