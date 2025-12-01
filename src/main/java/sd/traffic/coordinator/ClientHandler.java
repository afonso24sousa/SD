package sd.traffic.coordinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sd.traffic.common.LamportClock;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final CoordinatorServer server;
    private final Gson gson = new Gson();

    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, CoordinatorServer server) {
        this.socket = socket;
        this.server = server;
        setName("ClientHandler-" + socket.getRemoteSocketAddress());
    }

    private void sendOk(Object payload) {
        out.println(gson.toJson(new Message<>("OK", payload)));
    }

    private void sendError(String reason) {
        out.println(gson.toJson(new Message<>("ERROR", reason)));
    }

    @Override
    public void run() {
        try {

            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String line;

            while ((line = in.readLine()) != null) {

                Message<?> base = gson.fromJson(line, Message.class);
                if (base == null || base.getType() == null) {
                    System.out.println("[Coordinator] Mensagem inválida recebida: " + line);
                    continue;
                }

                // Atualizar relógio Lamport GLOBAL
                LamportClock clock = server.getClock();
                long lamport = clock.increment();

                switch (base.getType()) {

                    /* ============================================================ */
                    /* REGISTER                                                     */
                    /* ============================================================ */
                    case "REGISTER": {

                        JsonObject obj = gson.toJsonTree(base.getPayload()).getAsJsonObject();
                        RegisterRequest req = gson.fromJson(obj, RegisterRequest.class);

                        server.onRegister(req, socket);

                        logEvent(lamport, "REGISTER", req.getNodeId(), "role=" + req.getRole());
                        sendOk("REGISTER_OK");
                        break;
                    }


                    /* ============================================================ */
                    /* TELEMETRY (para dashboards)                                 */
                    /* ============================================================ */
                    case "TELEMETRY": {
                        TelemetryPayload tel = gson.fromJson(
                                gson.toJson(base.getPayload()), TelemetryPayload.class);

                        logEvent(lamport, "TELEMETRY", tel.getCrossing(), "queue=" + tel.getQueue());

                        // Broadcast
                        server.broadcastTelemetry(
                                gson.toJson(new Message<>("Telemetry", tel))
                        );

                        sendOk("TELEMETRY_OK");
                        break;
                    }

                    /* ============================================================ */
                    /* EVENT_LOG                                                    */
                    /* ============================================================ */
                    case "EVENT_LOG": {
                        EventLogEntry ev = gson.fromJson(
                                gson.toJson(base.getPayload()), EventLogEntry.class);

                        logDetailedEvent(lamport, ev);

                        // Registar no histórico por veículo
                        server.recordVehicleEvent(ev);

                        server.getEventList().addEvent(
                                new CoordinatorEvent(
                                        lamport, ev.getType(), ev.getNode(), ev.getDetails()
                                )
                        );

                        sendOk("EVENT_LOG_OK");
                        break;
                    }

                    /* ============================================================ */
                    /* TRAFFIC_STATS                                                */
                    /* ============================================================ */
                    case "TRAFFIC_STATS": {

                        TrafficStatsPayload stats = gson.fromJson(
                                gson.toJson(base.getPayload()), TrafficStatsPayload.class);

                        logEvent(lamport, "TRAFFIC_STATS", "SINK", gson.toJson(stats));

                        // Broadcast
                        Message<Object> msg = new Message<>("TRAFFIC_STATS", stats);

                        for (var entry : server.getDashboards().entrySet()) {
                            try {
                                PrintWriter dout = new PrintWriter(
                                        new OutputStreamWriter(entry.getValue().getOutputStream(), StandardCharsets.UTF_8), true);
                                dout.println(gson.toJson(msg));
                            } catch (Exception ex) {
                                System.err.println("[Coordinator] Erro ao enviar TRAFFIC_STATS para dashboard " + entry.getKey());
                            }
                        }

                        sendOk("STATS_OK");
                        break;
                    }

                    /* ============================================================ */
                    /* POLICY_UPDATE                                                */
                    /* ============================================================ */
                    case "POLICY_UPDATE": {

                        logEvent(lamport, "POLICY_UPDATE", "COORDINATOR", "reload request");

                        String policyJson = server.getCurrentPolicyJson();
                        out.println(gson.toJson(new Message<>("POLICY", policyJson)));

                        sendOk("POLICY_SENT");
                        break;
                    }

                    /* ============================================================ */
                    /* VehicleTransfer                                              */
                    /* ============================================================ */
                    case "VehicleTransfer": {

                        VehicleTransfer vt = gson.fromJson(
                                gson.toJson(base.getPayload()), VehicleTransfer.class);

                        logEvent(lamport, "VehicleTransfer", vt.getFrom(),
                                "vehicle=" + vt.getVehicleId() + " -> " + vt.getTo());

                        boolean ok = server.sendToNode(
                                vt.getTo(),
                                new Message<>("VehicleArrival", vt)
                        );

                        JsonObject ack = new JsonObject();
                        ack.addProperty("status", ok ? "OK" : "NODE_NOT_FOUND");
                        sendOk(ack);
                        break;
                    }

                    /* ============================================================ */
                    /* PHASE_REQUEST                                                */
                    /* ============================================================ */
                    case "PHASE_REQUEST": {
                        PhaseRequest req = gson.fromJson(
                                gson.toJson(base.getPayload()), PhaseRequest.class);

                        server.getPhaseController().requestGreen(req.getCrossing(), req.getDirection());

                        logEvent(lamport, "PHASE_REQUEST",
                                req.getCrossing(), "direction=" + req.getDirection());

                        sendOk("PHASE_GRANTED");
                        break;
                    }

                    /* ============================================================ */
                    /* PHASE_RELEASE                                                */
                    /* ============================================================ */
                    case "PHASE_RELEASE": {
                        PhaseRelease rel = gson.fromJson(
                                gson.toJson(base.getPayload()), PhaseRelease.class);

                        server.getPhaseController().releaseGreen(rel.getCrossing());

                        logEvent(lamport, "PHASE_RELEASE",
                                rel.getCrossing(), "direction=" + rel.getDirection());

                        sendOk("PHASE_RELEASED");
                        break;
                    }

                    /* ============================================================ */
                    /* REQUEST_HISTORY                                              */
                    /* ============================================================ */
                    case "REQUEST_HISTORY": {
                        String vid = (String) base.getPayload();
                        List<EventLogEntry> hist = server.getVehicleHistory(vid);

                        hist.sort(Comparator.comparingDouble(EventLogEntry::getSimTime));

                        out.println(gson.toJson(
                                new Message<>("HISTORY_DATA", hist)
                        ));
                        break;
                    }

                    /* ============================================================ */
                    /* DEFAULT                                                      */
                    /* ============================================================ */
                    default:
                        System.out.println("[Coordinator] Tipo desconhecido: " + base.getType());
                        sendError("UNKNOWN_TYPE");
                }
            }

            System.out.println("[ClientHandler] Cliente terminou ligação: " + socket.getRemoteSocketAddress());

        } catch (IOException e) {
            System.out.println("[ClientHandler] Cliente desconectado: " + socket.getRemoteSocketAddress());
        } finally {
            server.removeNode(socket);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /* ---------------------------------------------------------------------- */
    /* HELPERS                                                                */
    /* ---------------------------------------------------------------------- */

    private void logEvent(long lamport, String event, String node, String details) {
        JsonObject log = new JsonObject();
        log.addProperty("lamport", lamport);
        log.addProperty("event", event);
        log.addProperty("node", node);
        log.addProperty("details", details);

        server.appendEvent(log.toString());
        server.getEventList().addEvent(new CoordinatorEvent(lamport, event, node, details));
    }

    private void logDetailedEvent(long lamport, EventLogEntry ev) {
        JsonObject log = new JsonObject();
        log.addProperty("lamport", lamport);
        log.addProperty("event", ev.getType());
        log.addProperty("node", ev.getNode());
        log.addProperty("time", ev.getSimTime());
        log.addProperty("vehicleId", ev.getVehicleId());
        log.addProperty("details", ev.getDetails());
        server.appendEvent(log.toString());
    }
}
