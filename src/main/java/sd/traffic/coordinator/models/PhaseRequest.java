package sd.traffic.coordinator.models;

/**
 * Pedido de abertura de fase (semáforo verde) num cruzamento.
 * Enviado pelo Crossing para o Coordinator.
 */
public class PhaseRequest {

    private String crossing;
    private String direction;
    // opcional: timestamp lógico enviado pelo nó (pode vir a null nesta fase)
    private Long timestamp;

    public PhaseRequest() { }

    public PhaseRequest(String crossing, String direction, Long timestamp) {
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
        return "PhaseRequest{" +
                "crossing='" + crossing + '\'' +
                ", direction='" + direction + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
