package sd.traffic.coordinator.models;

/**
 * Pedido de libertar a fase (semáforo deixa de estar verde).
 */
public class PhaseRelease {

    private String crossing;
    private String direction;
    private Long timestamp;

    public PhaseRelease() { }

    public PhaseRelease(String crossing, String direction, Long timestamp) {
        this.crossing = crossing;
        this.direction = direction;
        this.timestamp = timestamp;
    }

    public String getCrossing() {
        return crossing;
    }

    public void setCrossing(String crossing) {
        this.crossing = crossing;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PhaseRelease{" +
                "crossing='" + crossing + '\'' +
                ", direction='" + direction + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
