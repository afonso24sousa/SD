package sd.traffic.dashboard;

import com.google.gson.Gson;
import sd.traffic.common.LinkIO;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.coordinator.models.TelemetryPayload;
import sd.traffic.coordinator.models.TrafficStatsPayload;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DashboardHub FINAL — versão robusta e alinhada com o Coordinator
 *
 * - Liga ao Coordinator
 * - Envia REGISTER("dashboard")
 * - Recebe:
 *     ✔ TELEMETRY (Crossings → Coordinator → Dashboard)
 *     ✔ TRAFFIC_STATS (Sink → Coordinator → Dashboard)
 * - Mantém telemetria por cruzamento
 * - Mantém estatísticas globais
 * - Imprime dashboard atualizado sempre que chega informação relevante
 */
public class DashboardHub {

    private static final String HOST = "127.0.0.1";
    private static final int COORDINATOR_PORT = 6000;

    /** Telemetria por cruzamento */
    private final ConcurrentHashMap<String, TelemetryPayload> telemetryMap =
            new ConcurrentHashMap<>();

    /** Estatísticas vindas do Sink */
    private volatile TrafficStatsPayload trafficStats = null;

    private final Gson gson = new Gson();

    public static void main(String[] args) {
        new DashboardHub().start();
    }

    public void start() {
        try {
            System.out.println("[DashboardHub] Connecting to Coordinator at "
                    + HOST + ":" + COORDINATOR_PORT);

            LinkIO link = new LinkIO(HOST, COORDINATOR_PORT);
            if (!link.connect()) {
                System.err.println("[DashboardHub] Could not connect to Coordinator.");
                return;
            }

            sendRegister(link);

            // ===========================
            //       MAIN RECEIVE LOOP
            // ===========================
            while (true) {
                String line = link.receive();
                if (line == null) {
                    System.err.println("[DashboardHub] Connection closed by Coordinator.");
                    break;
                }

                Message<?> msg = gson.fromJson(line, Message.class);
                if (msg == null || msg.getType() == null) {
                    System.err.println("[DashboardHub] Ignoring malformed message: " + line);
                    continue;
                }

                switch (msg.getType()) {

                    case "Telemetry": {
                        TelemetryPayload p = gson.fromJson(
                                gson.toJson(msg.getPayload()),
                                TelemetryPayload.class
                        );
                        telemetryMap.put(p.getCrossing(), p);
                        printDashboard();
                        break;
                    }

                    case "TRAFFIC_STATS": {
                        trafficStats = gson.fromJson(
                                gson.toJson(msg.getPayload()),
                                TrafficStatsPayload.class
                        );
                        printDashboard();
                        break;
                    }

                    case "STOP":
                        System.out.println("[DashboardHub] STOP recebido — a encerrar.");
                        return;


                    default:
                        System.out.println("[DashboardHub] Unhandled msg type: " + msg.getType());
                }
            }

        } catch (Exception e) {
            System.err.println("[DashboardHub] ERROR: " + e.getMessage());
        }
    }

    /** Envia REGISTER ao Coordinator */
    private void sendRegister(LinkIO link) {
        RegisterRequest req = new RegisterRequest();
        req.setNodeId("DashboardHub");
        req.setRole("dashboard");  // lowercase = mais consistente

        link.send(new Message<>("REGISTER", req));
        System.out.println("[DashboardHub] REGISTER sent.");
    }

    /** Imprime estado do dashboard (telemetria + estatísticas globais) */
    private void printDashboard() {
        System.out.println("\n================== DASHBOARD ==================");

        System.out.println("----- CROSSINGS STATE -----");

        telemetryMap.values().stream()
                .sorted(Comparator.comparing(TelemetryPayload::getCrossing))
                .forEach(t -> {
                    System.out.printf(
                            "%s | Queue: %-3d | Avg: %-4.2f | Light: %-6s | Ped: %s%n",
                            t.getCrossing(),
                            t.getQueue(),
                            t.getAvg(),
                            t.getLightState(),
                            t.isPedestrian() ? "YES" : "NO"
                    );
                });

        System.out.println("\n----- GLOBAL TRAFFIC STATS (SINK) -----");
        if (trafficStats == null) {
            System.out.println("No traffic stats received yet.");
        } else {
            System.out.println("Total vehicles: " + trafficStats.getTotalVehicles());

            System.out.println("Count by type:");
            trafficStats.getCountByType().forEach((type, count) ->
                    System.out.println("  " + type + ": " + count));

            System.out.println("\nAverage dwelling:");
            trafficStats.getAvgDwellingByType().forEach((type, avg) ->
                    System.out.printf("  %s: %.2f%n", type, avg));

            System.out.println("\nMin dwelling:");
            trafficStats.getMinDwellingByType().forEach((type, min) ->
                    System.out.printf("  %s: %.2f%n", type, min));

            System.out.println("\nMax dwelling:");
            trafficStats.getMaxDwellingByType().forEach((type, max) ->
                    System.out.printf("  %s: %.2f%n", type, max));
        }

        System.out.println("================================================\n");
    }
}
