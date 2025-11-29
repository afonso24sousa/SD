package sd.traffic.coordinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sd.traffic.common.LamportClock;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.common.ConfigLoader;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Servidor central (Coordinator).
 * - Ouve em TCP: porta configurável (default 6000)
 * - Aceita vários clientes (Crossings, Dashboard, Entry, Sink)
 * - Para cada ligação, lança uma Thread (ClientHandler)
 * - Mantém registo simples de nós registados (id -> Socket)
 * - Nesta fase: retransmite TELEMETRY recebida para todos os dashboards registados
 */
public class CoordinatorServer {

    private final int port;
    private final Gson gson = new Gson();

    /** Tabela de nós registados (Crossings, etc.). */
    private final Map<String, Socket> registeredNodes =
            Collections.synchronizedMap(new HashMap<>());

    /** Lista de Dashboards ligados — usada para broadcast de telemetria. */
    private final Map<String, Socket> dashboards =
            Collections.synchronizedMap(new HashMap<>());

    /** Gestor de políticas: lê policy_hybrid.json e fornece JSON a enviar. */
    private final PolicyManager policyManager = new PolicyManager();

    /** Store de eventos: append em logs/events.json */
    private final EventLogStore eventLogStore;

    private final PhaseController phaseController = new PhaseController();
    private final LamportClock clock = new LamportClock();

    public PhaseController getPhaseController() {
        return phaseController;
    }

    public LamportClock getClock() {
        return clock;
    }

    public static void main(String[] args) {
        new CoordinatorServer().start();
    }


    public CoordinatorServer() {
        JsonObject cfg = ConfigLoader.load("src/main/resources/config/default_config.json");
        int cfgPort = 6000;
        String logPath = "src/main/resources/logs/events.json";

        try {
            if (cfg.has("coordinator_port")) {
                cfgPort = cfg.get("coordinator_port").getAsInt();
            }
            if (cfg.has("logs_path")) {
                logPath = cfg.get("logs_path").getAsString();
            } else if (cfg.has("simulation")) {
                JsonObject sim = cfg.getAsJsonObject("simulation");
                if (sim.has("logs_path")) logPath = sim.get("logs_path").getAsString();
            }
        } catch (Exception ignore) {
            // Mantém defaults se algo não existir/for inválido
        }

        this.port = cfgPort;
        this.eventLogStore = new EventLogStore(logPath);
    }

    /** Inicia o servidor principal (multi-thread). */
    public void start() {
        System.out.println("[Coordinator] A iniciar na porta " + port + " ...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Coordinator] A ouvir em 0.0.0.0:" + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[Coordinator] Nova ligação de " + socket.getRemoteSocketAddress());
                new ClientHandler(socket, this).start();
            }
        } catch (IOException e) {
            System.err.println("[Coordinator] Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Regista um nó (Crossing ou Dashboard). */
    public void onRegister(RegisterRequest req, Socket socket) {
        if (req == null || req.getNodeId() == null) {
            System.out.println("[Coordinator] REGISTER inválido");
            return;
        }

        String id = req.getNodeId();

        String role = req.getRole();
        boolean isDashboard = role != null && role.equalsIgnoreCase("dashboard");

        // Distinguir tipo de nó pelo ID (ex: "DashboardHub")
        if (isDashboard){
            Socket prev = dashboards.put(id, socket);
            if (prev != null && !prev.isClosed()) {
                try {
                    prev.close();
                } catch (IOException ignore) { }
            }
            System.out.println("[Coordinator] Dashboard registado -> " + id);

        } else {

            Socket prev = registeredNodes.put(id, socket);
            if (prev != null && !prev.isClosed()) {
                try {
                    prev.close();
                } catch (IOException ignore) { }
            }
            System.out.println("[Coordinator] Crossing registado -> " + id);
        }
    }

    /** Broadcast de telemetria recebida aos dashboards. */
    public void broadcastTelemetry(String telemetryJson) {
        synchronized (dashboards) {
            for (Map.Entry<String, Socket> entry : dashboards.entrySet()) {
                try {
                    PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(entry.getValue().getOutputStream(), StandardCharsets.UTF_8), true);
                    out.println(telemetryJson);
                } catch (IOException e) {
                    System.err.println("[Coordinator] Falha ao enviar telemetria a " + entry.getKey());
                }
            }
        }
    }

    /** Obtém socket do nó registado */
    public Socket getNodeSocket(String id) {
        return registeredNodes.get(id);
    }

    /** Envia mensagem JSON para um nó específico */
    public boolean sendToNode(String nodeId, Object msg) {
        Socket s = registeredNodes.get(nodeId);
        if (s == null || s.isClosed()) {
            System.err.println("[Coordinator] Nó destino não encontrado: " + nodeId);
            return false;
        }

        try {
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),
                    true
            );
            out.println(gson.toJson(msg));
            return true;

        } catch (IOException e) {
            System.err.println("[Coordinator] Erro ao enviar a " + nodeId + ": " + e.getMessage());
            return false;
        }
    }

    /** Pede a política atual em JSON (carregada do ficheiro). */
    public String getCurrentPolicyJson() {
        return policyManager.getPolicyJson();
    }

    /** Permite atualizar política. */
    public void reloadPolicy() {
        policyManager.reload();
    }

    /** Regista evento em ficheiro. */
    public void appendEvent(String eventJsonLine) {
        eventLogStore.append(eventJsonLine);
    }

    /** Nós registados — útil no futuro para difundir mensagens. */
    public Map<String, Socket> getRegisteredNodes() {
        return registeredNodes;
    }

    public Map<String, Socket> getDashboards() {
        return dashboards;
    }
}
