package sd.traffic.coordinator.models;

/**
 * Entrada de log de eventos da simulação.
 *
 * Permite:
 *  - Registar tipo de evento (chegada, saída, mudança de fase, etc.)
 *  - Guardar o instante simulado
 *  - Indicar o nó (Cr1, Cr2, S, Entry, ...)
 *  - Associar opcionalmente um veículo específico (vehicleId)
 *  - Adicionar detalhes livres em texto/JSON
 *
 * Isto ajuda a:
 *  - Reconstruir o percurso completo de um veículo
 *  - Listar todos os eventos da simulação (requisito do enunciado)
 */
public class EventLogEntry {

    /** Tipo de evento: "VEHICLE_ARRIVAL", "VEHICLE_EXIT", "PHASE_CHANGE", ... */
    private String type;

    /** Tempo simulado em que o evento ocorreu. */
    private double simTime;

    /** Nó associado ao evento: "Cr1", "Cr2", "E1", "S", etc. */
    private String node;

    /** Identificador do veículo (pode ser null para eventos genéricos). */
    private String vehicleId;

    /** Campo livre para detalhes adicionais (texto ou JSON). */
    private String details;

    public EventLogEntry() {
    }

    /** Construtor original (sem vehicleId) — mantido para compatibilidade. */
    public EventLogEntry(String type, double simTime, String node, String details) {
        this.type = type;
        this.simTime = simTime;
        this.node = node;
        this.details = details;
    }

    /** Construtor completo com vehicleId. */
    public EventLogEntry(String type, double simTime, String node, String vehicleId, String details) {
        this.type = type;
        this.simTime = simTime;
        this.node = node;
        this.vehicleId = vehicleId;
        this.details = details;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getSimTime() { return simTime; }
    public void setSimTime(double simTime) { this.simTime = simTime; }

    public String getNode() { return node; }
    public void setNode(String node) { this.node = node; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return "EventLogEntry{" +
                "type='" + type + '\'' +
                ", simTime=" + simTime +
                ", node='" + node + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
