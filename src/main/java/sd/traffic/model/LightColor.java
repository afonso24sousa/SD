package sd.traffic.model;

/**
 * Estados possíveis de um semáforo.
 * Utilizado:
 *   - Nos Crossings (CrossingProcess)
 *   - No Dashboard
 *   - Na Telemetry (para mostrar estado atual)
 */
public enum LightColor {
    RED,
    YELLOW,
    GREEN,
    PEDESTRIAN
}
