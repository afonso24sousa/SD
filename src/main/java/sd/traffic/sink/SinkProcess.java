package sd.traffic.sink;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sd.traffic.common.LinkIO;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.coordinator.models.TrafficStatsPayload;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * SinkProcess — versão FINAL, robusta e alinhada com o teu formato real de EVENT_LOG.
 *
 * ✔ Lê apenas novas linhas (tail)
 * ✔ Guarda tipo de cada veículo (a partir de VEHICLE_ENTRY)
 * ✔ Processa VEHICLE_EXIT usando o tipo guardado
 * ✔ Mantém dwelling times corretos por tipo
 * ✔ Envia TRAFFIC_STATS ao Coordinator periodicamente
 */
public class SinkProcess {

    private final Gson gson = new Gson();
    private final LinkIO coordinator;

    private long lastRead = 0;
    private static final String LOG_PATH = "src/main/resources/logs/events.json";

    /** vehicleId → type */
    private final Map<String, String> vehicleTypeMap = new HashMap<>();

    /** dwelling times por tipo */
    private final Map<String, List<Double>> dwellingTimes = new HashMap<>();
    private final Map<String, Integer> countByType = new HashMap<>();

    private volatile boolean running = true;


    public SinkProcess() {

        coordinator = new LinkIO("localhost", 6000);

        dwellingTimes.put("CARRO", new ArrayList<>());
        dwellingTimes.put("MOTA", new ArrayList<>());
        dwellingTimes.put("CAMIAO", new ArrayList<>());

        countByType.put("CARRO", 0);
        countByType.put("MOTA", 0);
        countByType.put("CAMIAO", 0);
    }

    public void start() {

        if (!coordinator.connect()) {
            System.err.println("[Sink] Falha ao ligar ao Coordinator.");
            return;
        }

        sendRegister();

        new Thread(this::logReaderLoop, "Sink-LogReader").start();
        new Thread(this::statsSenderLoop, "Sink-StatsLoop").start();

        System.out.println("[Sink] Iniciado.");
    }

    private void sendRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setNodeId("SINK");
        req.setRole("SINK");
        coordinator.send(new Message<>("REGISTER", req));
    }

    // ===============================================================
    // Loop de leitura incremental dos logs
    // ===============================================================

    private void logReaderLoop() {
        while (running) {
            try {
                processNewLogLines();
                Thread.sleep(1000);
            } catch (Exception ignore) {}
        }
    }

    /** Lê apenas novas linhas (tail) */
    private void processNewLogLines() {

        try (BufferedReader br = new BufferedReader(new FileReader(LOG_PATH))) {

            long index = 0;
            String line;

            while ((line = br.readLine()) != null) {

                if (index++ < lastRead) continue;
                lastRead = index;

                JsonObject obj = gson.fromJson(line, JsonObject.class);
                if (obj == null || !obj.has("event")) continue;

                String event = obj.get("event").getAsString();

                switch (event) {
                    case "VEHICLE_ENTRY": handleVehicleEntry(obj); break;
                    case "VEHICLE_EXIT":  handleVehicleExit(obj);  break;
                }
            }

        } catch (Exception e) {
            System.err.println("[Sink] Erro a ler LOG: " + e.getMessage());
        }
    }

    // ===============================================================
    // EVENTO ENTRY
    // ===============================================================

    private void handleVehicleEntry(JsonObject obj) {

        if (!obj.has("vehicleId") || !obj.has("details")) return;

        String vehicleId = obj.get("vehicleId").getAsString();
        String details = obj.get("details").getAsString();

        String type = extractType(details);
        if (type != null) {
            vehicleTypeMap.put(vehicleId, type);
            System.out.println("[Sink] ENTRY " + vehicleId + " type=" + type);
        }
    }

    private String extractType(String details) {
        try {
            if (!details.contains("type=")) return null;

            String part = details.split("type=")[1];
            return part.split("[, ]")[0].trim();

        } catch (Exception ignore) {}

        return null;
    }

    // ===============================================================
    // EVENTO EXIT
    // ===============================================================

    private void handleVehicleExit(JsonObject obj) {

        if (!obj.has("vehicleId") || !obj.has("details")) return;

        String vehicleId = obj.get("vehicleId").getAsString();
        String details = obj.get("details").getAsString(); // "dwellingTime=X.X"

        double dwelling = extractDwelling(details);
        String type = vehicleTypeMap.get(vehicleId);

        if (type == null) {
            System.err.println("[Sink] ERRO: tipo não encontrado para " + vehicleId);
            return;
        }

        dwellingTimes.get(type).add(dwelling);
        countByType.put(type, countByType.get(type) + 1);

        System.out.println("[Sink] EXIT " + vehicleId +
                " type=" + type +
                " dwell=" + dwelling);
    }

    private double extractDwelling(String details) {
        try {
            return Double.parseDouble(details.split("=")[1].trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ===============================================================
    // Enviar estatísticas para o Coordinator
    // ===============================================================

    private void statsSenderLoop() {
        while (running) {
            try {
                String msg = coordinator.receiveNonBlocking();
                if ("STOP".equals(msg)) {
                    System.out.println("[Sink] STOP recebido — enviando estatísticas finais.");
                    TrafficStatsPayload finalStats = computeStats();
                    coordinator.send(new Message<>("TRAFFIC_STATS", finalStats));
                    running = false;
                    break;
                }

                Thread.sleep(3000);
                TrafficStatsPayload stats = computeStats();
                coordinator.send(new Message<>("TRAFFIC_STATS", stats));
            } catch (Exception ignore) {}
        }

        coordinator.close();
        System.out.println("[Sink] Encerrado.");
    }


    private TrafficStatsPayload computeStats() {

        TrafficStatsPayload p = new TrafficStatsPayload();

        int total = countByType.values().stream().mapToInt(i -> i).sum();
        p.setTotalVehicles(total);

        p.setCountByType(new HashMap<>(countByType));

        Map<String, Double> avg = new HashMap<>();
        Map<String, Double> min = new HashMap<>();
        Map<String, Double> max = new HashMap<>();

        for (String type : dwellingTimes.keySet()) {

            List<Double> list = dwellingTimes.get(type);

            if (list.isEmpty()) {
                avg.put(type, 0.0);
                min.put(type, 0.0);
                max.put(type, 0.0);
                continue;
            }

            double sum = list.stream().mapToDouble(v -> v).sum();
            double mn = list.stream().mapToDouble(v -> v).min().orElse(0);
            double mx = list.stream().mapToDouble(v -> v).max().orElse(0);

            avg.put(type, sum / list.size());
            min.put(type, mn);
            max.put(type, mx);
        }

        p.setAvgDwellingByType(avg);
        p.setMinDwellingByType(min);
        p.setMaxDwellingByType(max);

        return p;
    }

    public static void main(String[] args) {
        new SinkProcess().start();
    }
}
