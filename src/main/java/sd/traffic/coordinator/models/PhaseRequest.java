package sd.traffic.coordinator.models;

/**
 * Pedido de abertura de fase (semáforo verde) num cruzamento.
 *
 * Enviado pelo Crossing para o Coordinator, que usa PhaseController
 * para garantir exclusão mútua entre direções (NS vs EW vs peões).
 */
public class PhaseRequest {

    /** Identificador do cruzamento (ex.: "Cr1"). */
    private String crossing;

    /** Direção/local da fase pedida: "N", "S", "E", "W" ou "PEDESTRIAN". */
    private String direction;

    /** Timestamp lógico (Lamport) ou físico (opcional). */
    private Long timestamp;

    public PhaseRequest() { }

    public PhaseRequest(String crossing, String direction) {
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
        return "PhaseRequest{" +
                "crossing='" + crossing + '\'' +
                ", direction='" + direction + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
