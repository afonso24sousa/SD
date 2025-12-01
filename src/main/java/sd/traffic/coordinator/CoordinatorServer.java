package sd.traffic.coordinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sd.traffic.common.ConfigLoader;
import sd.traffic.common.LamportClock;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.EventLogEntry;
import sd.traffic.coordinator.models.RegisterRequest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoordinatorServer {

    private final int port;
    private final Gson gson = new Gson();

    private final Map<String, Socket> registeredNodes = new ConcurrentHashMap<>();
    private final Map<String, Socket> dashboards = new ConcurrentHashMap<>();

    private final PolicyManager policyManager = new PolicyManager();
    private final EventLogStore eventLogStore;
    private final PhaseController phaseController = new PhaseController();
    private final LamportClock clock = new LamportClock();

    private final CoordinatorEventList eventList = new CoordinatorEventList();

    private final Map<String, List<EventLogEntry>> vehicleHistory = new ConcurrentHashMap<>();

    public CoordinatorServer() {
        JsonObject cfg = ConfigLoader.loadDefaultConfig();

        int cfgPort = cfg.has("coordinator_port")
                ? cfg.get("coordinator_port").getAsInt()
                : 6000;

        String logPath = cfg.has("logs_path")
                ? cfg.get("logs_path").getAsString()
                : "src/main/resources/logs/events.json";

        this.port = cfgPort;
        this.eventLogStore = new EventLogStore(logPath);
    }

    public static void main(String[] args) {
        new CoordinatorServer().start();
    }

    public PhaseController getPhaseController() { return phaseController; }
    public LamportClock getClock() { return clock; }
    public PolicyManager getPolicyManager() { return policyManager; }
    public String getCurrentPolicyJson() { return policyManager.getPolicyJson(); }
    public CoordinatorEventList getEventList() { return eventList; }
    public Map<String, Socket> getRegisteredNodes() { return registeredNodes; }
    public Map<String, Socket> getDashboards() { return dashboards; }

    public void reloadPolicy() { policyManager.reload(); }

    // ============================
    // Histórico por veículo
    // ============================

    public void recordVehicleEvent(EventLogEntry ev) {
        String vid = ev.getVehicleId();
        if (vid == null) return;

        vehicleHistory
                .computeIfAbsent(vid, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(ev);
    }

    public List<EventLogEntry> getVehicleHistory(String vehicleId) {
        return vehicleHistory.getOrDefault(vehicleId, List.of());
    }

    // ============================
    // Servidor
    // ============================

    public void start() {
        System.out.println("[Coordinator] A iniciar na porta " + port + " ...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Coordinator] A ouvir em 0.0.0.0:" + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[Coordinator] Nova ligação: " + socket.getRemoteSocketAddress());
                new ClientHandler(socket, this).start();
            }

        } catch (IOException e) {
            System.err.println("[Coordinator] Erro no servidor: " + e.getMessage());
        }
    }

    // ============================
    // Registo e remoção de nós
    // ============================

    public void onRegister(RegisterRequest req, Socket socket) {
        if (req.getNodeId() == null) {
            System.out.println("[Coordinator] REGISTER inválido.");
            return;
        }

        String id = req.getNodeId();
        boolean isDashboard = "dashboard".equalsIgnoreCase(req.getRole());
        boolean isSink = "sink".equalsIgnoreCase(req.getRole());

        if (isDashboard) {
            dashboards.put(id, socket);
            System.out.println("[Coordinator] Dashboard registado: " + id);
        } else {
            registeredNodes.put(id, socket);
            System.out.println("[Coordinator] Nó registado: " + id + (isSink ? " (Sink)" : ""));
        }
    }

    public void removeNode(Socket socket) {
        registeredNodes.values().removeIf(s -> s == socket);
        dashboards.values().removeIf(s -> s == socket);
    }

    // ============================
    // Comunicação
    // ============================

    public void broadcastTelemetry(String json) {

        dashboards.entrySet().removeIf(e -> e.getValue().isClosed()); // ← CORREÇÃO

        for (var entry : dashboards.entrySet()) {
            try {
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(entry.getValue().getOutputStream(),
                                StandardCharsets.UTF_8), true);
                out.println(json);
            } catch (Exception ex) {
                System.err.println("[Coordinator] telemetria falhou → " + entry.getKey());
            }
        }
    }

    public boolean sendToNode(String nodeId, Object msg) {

        Socket s = registeredNodes.get(nodeId);
        if (s == null || s.isClosed()) {                      // ← CORREÇÃO
            registeredNodes.remove(nodeId);                  // ← CORREÇÃO
            return false;
        }

        try {
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),
                    true);
            out.println(gson.toJson(msg));
            return true;

        } catch (IOException e) {
            System.err.println("[Coordinator] Erro envio → " + nodeId);
            registeredNodes.remove(nodeId);                  // ← CORREÇÃO
            return false;
        }
    }

    // ============================
    // Logging
    // ============================

    public void appendEvent(String line) {
        eventLogStore.append(line);
    }
    //-----------------

    public void broadcastStop() {
        System.out.println("[Coordinator] STOP global enviado.");

        for (var entry : registeredNodes.entrySet()) {
            try {
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(entry.getValue().getOutputStream(), StandardCharsets.UTF_8), true);
                out.println(gson.toJson(new Message<>("STOP", "END_SIMULATION")));
            } catch (Exception e) {
                System.err.println("[Coordinator] Falha ao enviar STOP para " + entry.getKey());
            }
        }

        for (var entry : dashboards.entrySet()) {
            try {
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(entry.getValue().getOutputStream(), StandardCharsets.UTF_8), true);
                out.println(gson.toJson(new Message<>("STOP", "END_SIMULATION")));
            } catch (Exception e) {
                System.err.println("[Coordinator] Falha ao enviar STOP para dashboard.");
            }
        }
    }

}
