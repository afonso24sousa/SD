package sd.traffic.coordinator.models;

/**
 * Payload para a mensagem REGISTER.
 *
 * Exemplo:
 * { "type":"REGISTER", "payload": { "nodeId":"Cr1", "role":"CROSSING" } }
 *
 * role:
 *  - "CROSSING"
 *  - "DASHBOARD"
 *  - "SINK"
 *  - "ENTRY"
 */
public class RegisterRequest {

    private String nodeId; // ex.: "Cr1", "DashboardHub", "Sink", "E1"
    private String role;   // "CROSSING", "DASHBOARD", "SINK", "ENTRY"

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
