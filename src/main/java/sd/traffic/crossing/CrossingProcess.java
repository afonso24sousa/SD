package sd.traffic.crossing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sd.traffic.common.*;
import sd.traffic.coordinator.models.*;
import sd.traffic.model.LightColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CrossingProcess — VERSÃO FINAL CORRIGIDA
 *
 * - Integra PhaseController GLOBAL (PHASE_REQUEST / PHASE_RELEASE)
 * - Semáforos N/S/E/W por thread
 * - PedestrianSemaphore automático apenas no Cr4
 * - Telemetria completa (fila total e média, estado da luz)
 * - Receção eficiente via receiveNonBlocking()
 * - Encaminhamento correto VehicleArrival → fila
 * - forwardVehicle → VehicleTransfer ou EXIT
 */

public class CrossingProcess {

    private final String id;
    private final int localPort;

    private final Gson gson = new Gson();
    private final LinkIO linkCoordinator;

    private final Map<String, VehicleQueue> queues = new HashMap<>();
    private final Map<String, CrossingSemaphore> semaphores = new HashMap<>();
    private PedestrianSemaphore pedestrianSemaphore;

    private final SimClock simClock = new SimClock();

    private final double tRoad;
    private final double tSem;

    private volatile LightColor currentLight = LightColor.RED;
    private volatile Policy localPolicy = null;

    private final long telemetryIntervalMs = 2000L;
    private volatile boolean running = true;

    private ServerSocket serverSocket;


    // =====================================================================
    // CONSTRUCTOR
    // =====================================================================

    public CrossingProcess(String id, int port) {
        this.id = id;
        this.localPort = port;
        this.linkCoordinator = new LinkIO("localhost", 6000);

        JsonObject cfg = ConfigLoader.loadDefaultConfig();
        JsonObject simCfg = cfg.getAsJsonObject("simulation");

        double tmpRoad = 3.0;
        double tmpSem = 1.5;

        try {
            if (simCfg.has("t_road")) tmpRoad = simCfg.get("t_road").getAsDouble();
            if (simCfg.has("t_sem")) tmpSem = simCfg.get("t_sem").getAsDouble();
        } catch (Exception ignore) {}

        this.tRoad = tmpRoad;
        this.tSem = tmpSem;

        queues.put("N", new VehicleQueue());
        queues.put("S", new VehicleQueue());
        queues.put("E", new VehicleQueue());
        queues.put("W", new VehicleQueue());

        semaphores.put("N", new CrossingSemaphore(id, "N", queues.get("N"), this, simClock, tSem));
        semaphores.put("S", new CrossingSemaphore(id, "S", queues.get("S"), this, simClock, tSem));
        semaphores.put("E", new CrossingSemaphore(id, "E", queues.get("E"), this, simClock, tSem));
        semaphores.put("W", new CrossingSemaphore(id, "W", queues.get("W"), this, simClock, tSem));

        if ("Cr4".equals(id)) {
            pedestrianSemaphore = new PedestrianSemaphore(
                    id,
                    this,
                    simClock,
                    4.0,
                    12000
            );
        }
    }


    // =====================================================================
    // START
    // =====================================================================

    public void start() {

        if (!linkCoordinator.connect()) {
            System.err.println("[Crossing " + id + "] Falha ao ligar ao Coordinator.");
            return;
        }

        sendRegister();
        requestInitialPolicy();

        new Thread(this::startServerSocket, "CrossingServer-" + id).start();
        new Thread(this::telemetryLoop, "Telemetry-" + id).start();
        new Thread(this::listenCoordinator, "CoordinatorListener-" + id).start();

        semaphores.values().forEach(Thread::start);

        if (pedestrianSemaphore != null)
            pedestrianSemaphore.start();
    }


    // =====================================================================
    // REGISTER
    // =====================================================================

    private void sendRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setNodeId(id);
        req.setRole("CROSSING");
        linkCoordinator.send(new Message<>("REGISTER", req));
    }

    private void requestInitialPolicy() {
        linkCoordinator.send(new Message<>("POLICY_UPDATE", "REQUEST_POLICY"));
    }


    // =====================================================================
    // SERVER SOCKET — RECEÇÃO DE VehicleArrival
    // =====================================================================

    private void startServerSocket() {
        try {
            serverSocket = new ServerSocket(localPort);
            serverSocket.setSoTimeout(500);

            System.out.println("[Crossing " + id + "] A ouvir em " + localPort);

            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    new Thread(() -> handleIncoming(s), "CrossingIncoming-" + id).start();
                } catch (SocketTimeoutException ignore) {
                    // apenas acorda para verificar running
                }
            }

        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Server error: " + e.getMessage());
        }
    }

    private void handleIncoming(Socket s) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {

                Message<?> msg = gson.fromJson(line, Message.class);
                if (msg == null || msg.getType() == null) continue;

                if ("VehicleArrival".equals(msg.getType())) {

                    VehicleTransfer vt = gson.fromJson(
                            gson.toJson(msg.getPayload()),
                            VehicleTransfer.class
                    );

                    Vehicle v = new Vehicle();
                    v.setId(vt.getVehicleId());
                    v.setPath(vt.getPath());
                    v.setPathIndex(vt.getIndex());
                    v.setType(vt.getType());
                    v.setEnteredAtSimTime(vt.getTime());

                    String direction = inferDirection(vt.getFrom(), vt.getTo());
                    if (direction == null) {
                        System.err.println("[Crossing " + id + "] Edge não mapeado: "
                                + vt.getFrom() + ">" + vt.getTo());
                        continue;
                    }

                    queues.get(direction).enqueue(v);

                    EventLogEntry ev = new EventLogEntry(
                            "VEHICLE_ARRIVAL",
                            simClock.getSimTime(),
                            id,
                            v.getId(),
                            "from=" + vt.getFrom() + ", to=" + vt.getTo() + ", dir=" + direction
                    );
                    linkCoordinator.send(new Message<>("EVENT_LOG", ev));
                }
            }

        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Incoming error: " + e.getMessage());
        }
    }


    // =====================================================================
    // LISTEN COORDINATOR (POLICY + STOP)
    // =====================================================================

    private void listenCoordinator() {
        try {
            while (running) {

                String line = linkCoordinator.receiveNonBlocking();
                if (line == null) {
                    Thread.sleep(40);
                    continue;
                }

                Message<?> msg = gson.fromJson(line, Message.class);
                if (msg == null || msg.getType() == null) continue;

                switch (msg.getType()) {

                    case "POLICY":
                        String jsonPolicy = (String) msg.getPayload();
                        localPolicy = gson.fromJson(jsonPolicy, Policy.class);
                        System.out.println("[Crossing " + id + "] Política atualizada: " + localPolicy);
                        break;

                    case "STOP":
                        System.out.println("[Crossing " + id + "] STOP recebido — a encerrar.");
                        shutdown();
                        return;
                }
            }

        } catch (Exception e) {
            System.err.println("[Crossing " + id + "] Erro leitura Coordinator: " + e.getMessage());
        }
    }


    public Policy getPolicy() {
        return localPolicy;
    }


    // =====================================================================
    // DIREÇÕES (from→to)
    // =====================================================================

    private String inferDirection(String from, String to) {
        switch (from + ">" + to) {

            case "Cr1>Cr2": return "S";
            case "Cr2>Cr1": return "N";

            case "Cr2>Cr3": return "S";
            case "Cr3>Cr2": return "N";

            case "Cr1>Cr4": return "E";
            case "Cr4>Cr1": return "W";

            case "Cr4>Cr5": return "S";
            case "Cr5>Cr4": return "N";

            case "Cr2>Cr5": return "E";
            case "Cr5>Cr2": return "W";

            default:
                return null;
        }
    }


    // =====================================================================
    // PHASE CONTROLLER GLOBAL
    // =====================================================================

    public void requestGreenGlobal(String direction) {
        linkCoordinator.send(new Message<>("PHASE_REQUEST", new PhaseRequest(id, direction)));
    }

    public void releaseGreenGlobal(String direction) {
        linkCoordinator.send(new Message<>("PHASE_RELEASE", new PhaseRelease(id, direction)));
    }

    public void requestPedestrianGreenGlobal() {
        linkCoordinator.send(new Message<>("PHASE_REQUEST", new PhaseRequest(id, "PEDESTRIAN")));
    }

    public void releasePedestrianGreenGlobal() {
        linkCoordinator.send(new Message<>("PHASE_RELEASE", new PhaseRelease(id, "PEDESTRIAN")));
    }


    public LinkIO getCoordinatorLink() {
        return linkCoordinator;
    }


    // =====================================================================
    // FORWARD VEHICLE
    // =====================================================================

    public void forwardVehicle(Vehicle v) {

        List<NodeId> path = v.getPath();
        int nextIndex = v.getPathIndex() + 1;

        if (nextIndex >= path.size()) {

            v.setLeftAtSimTime(simClock.getSimTime());

            EventLogEntry ev = new EventLogEntry(
                    "VEHICLE_EXIT",
                    simClock.getSimTime(),
                    id,
                    v.getId(),
                    "dwellingTime=" + v.getDwellingTime()
            );
            linkCoordinator.send(new Message<>("EVENT_LOG", ev));
            return;
        }

        NodeId next = path.get(nextIndex);

        double simDelta = tRoad * v.getType().getFactor();

        try {
            Thread.sleep(simClock.toRealMillis(simDelta));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        simClock.advance(simDelta);

        VehicleTransfer vt = new VehicleTransfer(
                v.getId(),
                id,
                next.name(),
                simClock.getSimTime(),
                path,
                nextIndex,
                v.getType()
        );

        linkCoordinator.send(new Message<>("VehicleTransfer", vt));
    }


    // =====================================================================
    // TELEMETRIA
    // =====================================================================

    private void telemetryLoop() {
        while (running) {
            try {
                queues.values().forEach(VehicleQueue::sample);

                TelemetryPayload t = new TelemetryPayload();
                t.setCrossing(id);

                int totalQueue = queues.values().stream().mapToInt(VehicleQueue::size).sum();

                double avgQueue = queues.values()
                        .stream()
                        .mapToDouble(VehicleQueue::getAverageSize)
                        .average()
                        .orElse(0.0);

                LightColor light = isCarPhaseActive() ? LightColor.GREEN : LightColor.RED;
                currentLight = light;

                t.setQueue(totalQueue);
                t.setAvg(avgQueue);
                t.setLightState(light);
                t.setPedestrian(false);

                linkCoordinator.send(new Message<>("TELEMETRY", t));

                Thread.sleep(telemetryIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    public boolean isCarPhaseActive() {
        return semaphores.values().stream()
                .anyMatch(CrossingSemaphore::isProcessing);
    }


    // =====================================================================
    // SHUTDOWN
    // =====================================================================

    public void shutdown() {
        running = false;

        try { semaphores.values().forEach(CrossingSemaphore::shutdown); } catch (Exception ignored) {}
        try { if (pedestrianSemaphore != null) pedestrianSemaphore.shutdown(); } catch (Exception ignored) {}
        try { linkCoordinator.close(); } catch (Exception ignored) {}

        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}

        System.out.println("[Crossing " + id + "] Encerrado.");
    }


    // =====================================================================
    // MAIN
    // =====================================================================

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: CrossingProcess <ID> <PORT>");
            return;
        }
        new CrossingProcess(args[0], Integer.parseInt(args[1])).start();
    }
}
