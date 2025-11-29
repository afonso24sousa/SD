package sd.traffic.coordinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sd.traffic.common.Event;
import sd.traffic.coordinator.models.*;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.VehicleTransfer;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Thread por cliente (segue a ficha de servidor TCP multi-thread):
 *  - Lê linhas JSON do cliente
 *  - Trata: REGISTER, TELEMETRY, EVENT_LOG, POLICY_UPDATE
 *  - Responde com JSON em linha única
 *  - A partir da Fase 2: reencaminha TELEMETRY para os Dashboards
 */
public class ClientHandler extends Thread {

    private final Socket socket;
    private final CoordinatorServer server;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket, CoordinatorServer server) {
        this.socket = socket;
        this.server = server;
        setName("ClientHandler-" + socket.getRemoteSocketAddress());
    }

    // helpers para respostas consistentes
    private void sendOk(PrintWriter out, Object payload) {
        out.println(gson.toJson(new Message<>("OK", payload)));
    }

    private void sendError(PrintWriter out, String reason) {
        out.println(gson.toJson(new Message<>("ERROR", reason)));
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Message<?> base = gson.fromJson(line, Message.class);
                    if (base == null || base.getType() == null) {
                        System.out.println("[Coordinator] Mensagem inválida: " + line);
                        continue;
                    }

                    switch (base.getType()) {
                        case "REGISTER": {
                            RegisterRequest req = gson.fromJson(gson.toJson(base.getPayload()), RegisterRequest.class);
                            server.onRegister(req, socket);

                            JsonObject regLog = new JsonObject();
                            regLog.addProperty("type", "REGISTER");
                            regLog.addProperty("nodeId", req.getNodeId());
                            server.appendEvent(regLog.toString());

                            JsonObject ok = new JsonObject();
                            ok.addProperty("status", "OK");
                            ok.addProperty("msg", "REGISTER_OK");
                            sendOk(out, ok);
                            break;
                        }

                        case "TELEMETRY": {
                            TelemetryPayload tel = gson.fromJson(gson.toJson(base.getPayload()), TelemetryPayload.class);
                            System.out.println("[Telemetry] " + tel);

                            String telemetryJson = gson.toJson(new EventLogEntry(
                                    "TELEMETRY",
                                    0.0,
                                    tel.getCrossing(),
                                    gson.toJson(tel)
                            ));

                            // Guarda no log
                            server.appendEvent(telemetryJson);

                            // Reencaminha para Dashboards
                            server.broadcastTelemetry(gson.toJson(new Message<>("Telemetry", tel)));

                            sendOk(out, "TELEMETRY_OK");
                            break;
                        }

                        case "EVENT_LOG": {
                            EventLogEntry ev = gson.fromJson(gson.toJson(base.getPayload()), EventLogEntry.class);
                            server.appendEvent(gson.toJson(ev));
                            sendOk(out, "EVENT_LOG_OK");
                            break;
                        }

                        case "POLICY_UPDATE": {
                            String policyJson = server.getCurrentPolicyJson();
                            out.println(gson.toJson(new Message<>("POLICY", policyJson)));
                            break;
                        }

                        case "VehicleTransfer": {
                            VehicleTransfer vt = gson.fromJson(gson.toJson(base.getPayload()), VehicleTransfer.class);

                            server.appendEvent(gson.toJson(vt));

                            // reencaminhar ao cruzamento de destino
                            boolean sent = server.sendToNode(vt.getTo(),
                                    new Message<>("VehicleArrival", vt));

                            JsonObject okV = new JsonObject();
                            okV.addProperty("status", sent ? "VEHICLE_TRANSFER_OK" : "DESTINATION_NOT_FOUND");
                            sendOk(out, okV);

                            break;
                        }


                        case "PHASE_REQUEST": {
                            PhaseRequest req = gson.fromJson(gson.toJson(base.getPayload()), PhaseRequest.class);

                            server.getPhaseController().requestGreen(req.getCrossing(), req.getDirection());

                            // envio ACK
                            JsonObject ack = new JsonObject();
                            ack.addProperty("status", "PHASE_GRANTED");
                            sendOk(out, ack);
                            break;
                        }

                        case "PHASE_RELEASE": {
                            PhaseRelease rel = gson.fromJson(gson.toJson(base.getPayload()), PhaseRelease.class);

                            server.getPhaseController().releaseGreen(rel.getCrossing());

                            JsonObject ack = new JsonObject();
                            ack.addProperty("status", "PHASE_RELEASED");
                            sendOk(out, ack);
                            break;
                        }

                        default: {
                            System.out.println("[Coordinator] Tipo desconhecido: " + base.getType());
                            sendError(out, "UNKNOWN_TYPE");
                        }
                    }
                }
                System.out.println("[ClientHandler] Cliente terminou ligação: " + socket.getRemoteSocketAddress());

            } catch (IOException ignored) {
                System.out.println("[ClientHandler] Cliente desconectado: " + socket.getRemoteSocketAddress());
            }

        } catch (IOException e) {
            System.err.println("[Coordinator] Erro no socket: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) { }
        }
    }
}