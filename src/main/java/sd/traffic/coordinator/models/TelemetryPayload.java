package sd.traffic.coordinator.models;

import sd.traffic.model.LightColor;

/**
 * Payload para TELEMETRY.
 *
 * Exemplo:
 * {
 *   "type":"Telemetry",
 *   "crossing":"Cr1",
 *   "queue":4,
 *   "avg":2.5,
 *   "lightState":"GREEN",
 *   "pedestrian": false,
 *   "directionGreen": "N"
 * }
 *
 * Campos usados pelo Dashboard:
 *  - crossing: ID do cruzamento
 *  - queue: soma das filas
 *  - avg: média das filas (opcional)
 *  - lightState: estado geral (GREEN/RED)
 *  - pedestrian: se o semáforo de peões está verde (apenas Cr4)
 *  - directionGreen: qual direção está verde (N/S/E/W) — útil para debug
 */
public class TelemetryPayload {

    /** Identificador do cruzamento (ex.: "Cr1"). */
    private String crossing;

    /** Tamanho total da fila (soma direções N/S/E/W). */
    private int queue;

    /** Tempo médio (pode ser 0.0). */
    private double avg;

    /** Estado global do semáforo. */
    private LightColor lightState;

    /** Semáforo de peões está verde? (Só Cr4 usa, mas enviado para todos) */
    private boolean pedestrian;

    /** Qual direção de carros está verde (N/S/E/W). Null significa nenhuma. */
    private String directionGreen;

    // ========================
    // GETTERS / SETTERS
    // ========================

    public String getCrossing() { return crossing; }
    public void setCrossing(String crossing) { this.crossing = crossing; }

    public int getQueue() { return queue; }
    public void setQueue(int queue) { this.queue = Math.max(0, queue); }

    public double getAvg() { return avg; }
    public void setAvg(double avg) { this.avg = avg; }

    public LightColor getLightState() { return lightState; }
    public void setLightState(LightColor lightState) { this.lightState = lightState; }

    public boolean isPedestrian() { return pedestrian; }
    public void setPedestrian(boolean pedestrian) { this.pedestrian = pedestrian; }

    public String getDirectionGreen() { return directionGreen; }
    public void setDirectionGreen(String directionGreen) { this.directionGreen = directionGreen; }

    @Override
    public String toString() {
        return "TelemetryPayload{" +
                "crossing='" + crossing + '\'' +
                ", queue=" + queue +
                ", avg=" + avg +
                ", lightState=" + lightState +
                ", pedestrian=" + pedestrian +
                ", directionGreen='" + directionGreen + '\'' +
                '}';
    }
}
