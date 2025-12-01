package sd.traffic.coordinator.models;

/**
 * Pedido de libertar a fase (semáforo deixa de estar verde) num cruzamento.
 *
 * Enviado pelo Crossing para o Coordinator, que usa o PhaseController
 * para libertar o grupo de direções e acordar outros semáforos.
 */
public class PhaseRelease {

    /** Identificador do cruzamento (ex.: "Cr1"). */
    private String crossing;

    /** Direção/local lógica que está a ser libertada (ex.: "N", "S", "E", "W", "PEDESTRIAN"). */
    private String direction;

    /** Timestamp lógico ou físico (opcional). Pode ser null. */
    private Long timestamp;

    public PhaseRelease() { }

    public PhaseRelease(String crossing, String direction) {
        this.crossing = crossing;
        this.direction = direction;
        this.timestamp = null;
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
