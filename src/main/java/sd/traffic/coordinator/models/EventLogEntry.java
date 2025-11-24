package sd.traffic.coordinator.models;

/**
 * Payload para EVENT_LOG — simples e genérico nesta fase.
 * Estender futuramente (tipo, simTime, vehicleId, node, etc.)
 */
public class EventLogEntry {
    private String type;      // p.ex. "VEHICLE_ARRIVAL", "START_GREEN", ...
    private double simTime;   // timestamp simulado (fase 3+)
    private String node;      // "Cr1", "Cr2", "S", etc.
    private String details;   // livre

    public EventLogEntry(String type, double simTime, String node, String details) {
        this.type = type;
        this.simTime = simTime;
        this.node = node;
        this.details = details;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getSimTime() { return simTime; }
    public void setSimTime(double simTime) { this.simTime = simTime; }

    public String getNode() { return node; }
    public void setNode(String node) { this.node = node; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
