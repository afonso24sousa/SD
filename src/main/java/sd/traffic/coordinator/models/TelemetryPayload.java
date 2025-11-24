package sd.traffic.coordinator.models;
import sd.traffic.model.LightColor;


/**
 * Payload para TELEMETRY.
 * Exemplo do enunciado do contrato:
 * { "type":"Telemetry", "crossing":"Cr1", "queue":4, "avg":2.5 }
 */
public class TelemetryPayload {
    private String crossing;
    private int queue;
    private double avg;
    private LightColor lightState;

    public String getCrossing() { return crossing; }
    public void setCrossing(String crossing) { this.crossing = crossing; }

    public int getQueue() { return queue; }
    public void setQueue(int queue) {
        this.queue = Math.max(0, queue);
    }

    public double getAvg() { return avg; }
    public void setAvg(double avg) { this.avg = avg; }

    public LightColor getLightState() { return lightState; }
    public void setLightState(LightColor lightState) { this.lightState = lightState; }

    @Override
    public String toString() {
        return "TelemetryPayload{" +
                "crossing='" + crossing + '\'' +
                ", queue=" + queue +
                ", avg=" + avg +
                ", lightState=" + lightState + '\'' +
                '}';
    }
}
