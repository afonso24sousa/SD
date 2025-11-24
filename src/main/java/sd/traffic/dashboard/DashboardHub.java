package sd.traffic.dashboard;

import com.google.gson.Gson;
import sd.traffic.common.LinkIO;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.coordinator.models.TelemetryPayload;

import java.util.concurrent.ConcurrentHashMap;

/**
 * DashboardHub (versão correta para a Fase 2):
 *
 * - Liga-se ao Coordinator (porta 6000)
 * - Envia REGISTER {"nodeId": "DashboardHub"}
 * - Recebe mensagens "Telemetry" enviadas via broadcast
 * - Atualiza visualização textual com a informação dos cruzamentos
 *
 */
public class DashboardHub {

    private static final String HOST = "127.0.0.1";
    private static final int COORDINATOR_PORT = 6000;

    private final ConcurrentHashMap<String, TelemetryPayload> telemetryMap =
            new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    public static void main(String[] args) {
        new DashboardHub().start();
    }

    public void start() {
        try {
            System.out.println("[DashboardHub] Connecting to Coordinator at " + HOST + ":" + COORDINATOR_PORT);

            // Ligação TCP usando LinkIO
            LinkIO link = new LinkIO(HOST, COORDINATOR_PORT);
            link.connect();

            // Enviar REGISTER
            RegisterRequest reg = new RegisterRequest();
            reg.setNodeId("DashboardHub");
            reg.setRole("dashboard");

            link.send(new Message<>("REGISTER", reg));
            System.out.println("[DashboardHub] Sent REGISTER");

            // Loop principal: fica à espera de Telemetry
            while (true) {
                String line = link.receive();
                if (line == null) {
                    break;
                }

                Message<?> msg = gson.fromJson(line, Message.class);

                if ("Telemetry".equalsIgnoreCase(msg.getType())) {
                    TelemetryPayload payload =
                            gson.fromJson(gson.toJson(msg.getPayload()), TelemetryPayload.class);

                    telemetryMap.put(payload.getCrossing(), payload);
                    printDashboard();
                }
            }

        } catch (Exception e) {
            System.err.println("[DashboardHub] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printDashboard() {
        System.out.println("\n=== DASHBOARD ===");
        telemetryMap.forEach((crossing, data) -> {
            System.out.printf(
                    "%s | Queue: %d | Avg: %.2f | Light: %s%n",
                    crossing, data.getQueue(), data.getAvg(), data.getLightState()
            );
        });
    }
}
