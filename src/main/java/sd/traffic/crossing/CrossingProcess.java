package sd.traffic.crossing;

import com.google.gson.Gson;
import sd.traffic.common.*;
import sd.traffic.coordinator.models.*;
import sd.traffic.model.LightColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * FASE 3 / 4 / 5 - Membro B
 *
 * Neste ponto o Crossing faz:
 *  - registo no Coordinator (REGISTER)
 *  - mantém QUATRO filas por direção (N, S, E, W)
 *  - recebe VehicleArrival (enviado pelo Coordinator quando recebe VehicleTransfer)
 *  - determina direção aproximada do veículo (N/S) a partir de (from,to)
 *  - faz pedido PHASE_REQUEST ao Coordinator (que usa PhaseController para exclusão mútua)
 *  - processa o veículo com tempo simulado
 *  - liberta a fase com PHASE_RELEASE
 *  - envia EVENT_LOG para registar saída do cruzamento
 *  - envia telemetria real (tamanho das filas + estado da luz)
 *
 * Na Fase 5 o encaminhamento é real seguindo o path completo do veículo.
 */
public class CrossingProcess {

    private final String id;
    private final int localPort;

    private final Gson gson = new Gson();
    private final LinkIO linkCoordinator;

    /** Filas por direção — AGORA thread-safe manualmente usando synchronized+wait/notify */
    private final Map<String, VehicleQueue> queues = new HashMap<>();

    private volatile LightColor currentLight = LightColor.RED;
    private final SimClock simClock = new SimClock();

    private final double tRoad = 3.0;
    private final long telemetryIntervalMs = 2000;

    public CrossingProcess(String id, int port) {
        this.id = id;
        this.localPort = port;
        this.linkCoordinator = new LinkIO("localhost", 6000);

        // Criar 4 filas thread-safe
        queues.put("N", new VehicleQueue());
        queues.put("S", new VehicleQueue());
        queues.put("E", new VehicleQueue());
        queues.put("W", new VehicleQueue());
    }

    /** Arranque do processo */
    public void start() {
        if (!linkCoordinator.connect()) {
            System.err.println("[Crossing " + id + "] Falha ao ligar ao Coordinator.");
            return;
        }

        sendRegister();

        new Thread(this::startServerSocket, "CrossingServer-" + id).start();
        new Thread(this::telemetryLoop, "Telemetry-" + id).start();
        new Thread(this::loopProcessamento, "Processor-" + id).start();
        new Thread(this::listenCoordinator, "CoordinatorListener-" + id).start();
    }

    /** Envia REGISTER ao Coordinator */
    private void sendRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setNodeId(id);
        req.setRole("CROSSING");
        linkCoordinator.send(new Message<>("REGISTER", req));
        System.out.println("[Crossing " + id + "] REGISTER enviado ao Coordinator.");
    }

    /** ServerSocket: recebe VehicleArrival do Coordinator */
    private void startServerSocket() {
        try (ServerSocket ss = new ServerSocket(localPort)) {
            System.out.println("[Crossing " + id + "] A ouvir em " + localPort);

            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handleIncoming(s),
                        "Incoming-" + id + "-" + s.getRemoteSocketAddress()).start();
            }

        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Erro no server socket: " + e.getMessage());
        }
    }

    /** Recebe VehicleArrival e coloca na fila correta */
    private void handleIncoming(Socket s) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            String line;
            while ((line = in.readLine()) != null) {

                Message<?> msg = gson.fromJson(line, Message.class);
                if (msg == null || msg.getType() == null) continue;

                if (msg.getType().equals("VehicleArrival")) {

                    VehicleTransfer vt =
                            gson.fromJson(gson.toJson(msg.getPayload()), VehicleTransfer.class);

                    Vehicle v = new Vehicle();
                    v.setId(vt.getVehicleId());
                    v.setPath(vt.getPath());
                    v.setPathIndex(vt.getIndex());
                    v.setType(vt.getType());
                    v.setEnteredAtSimTime(vt.getTime());

                    List<NodeId> path = vt.getPath();
                    int idx = vt.getIndex();

                    String direction =
                            inferDirection(path.get(idx - 1).name(), path.get(idx).name());

                    VehicleQueue q = queues.get(direction);
                    if (q == null) {
                        System.err.println("[Crossing " + id + "] Direção inválida: " + direction);
                        continue;
                    }

                    q.enqueue(v);
                    System.out.println("[Crossing " + id + "] Veículo " + v.getId() +
                            " entrou pela direção " + direction);
                }
            }

        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Erro incoming: " + e.getMessage());
        }
    }

    /** Direção estimada */
    private String inferDirection(String from, String to) {
        try {
            NodeId f = NodeId.valueOf(from);
            NodeId t = NodeId.valueOf(to);
            return (f.ordinal() < t.ordinal()) ? "N" : "S";
        } catch (Exception e) {
            return (from.compareTo(to) < 0) ? "N" : "S";
        }
    }

    /**
     * Loop principal:
     * - Espera veículo em fila usando wait()
     * - Pede fase
     * - Simula tempo
     * - Encaminha ou regista saída
     * - Liberta fase
     */
    private void loopProcessamento() {
        while (true) {
            try {
                String direction = pickDirection();
                if (direction == null) {
                    Thread.sleep(20);
                    continue;
                }

                VehicleQueue q = queues.get(direction);
                if (q == null) continue;

                // poll() BLOQUEIA com wait() até haver veículos
                Vehicle v = q.poll();

                sendPhaseRequest(direction);
                currentLight = LightColor.GREEN;

                double factor = v.getType().getFactor();
                double simDelta = tRoad * factor;
                Thread.sleep(simClock.toRealMillis(simDelta));
                simClock.advance(simDelta);

                forwardVehicle(v);

                sendPhaseRelease(direction);
                currentLight = LightColor.RED;

            } catch (Exception e) {
                System.err.println("[Crossing " + id + "] Erro loopProcessamento: " + e.getMessage());
            }
        }
    }

    /** Escolhe uma direção com fila não vazia (sem bloquear) */
    private String pickDirection() {
        for (var e : queues.entrySet()) {
            if (!e.getValue().isEmpty()) return e.getKey();
        }
        return null;
    }

    private void sendPhaseRequest(String dir) {
        PhaseRequest pr = new PhaseRequest(id, dir, null);
        linkCoordinator.send(new Message<>("PHASE_REQUEST", pr));
    }

    private void sendPhaseRelease(String dir) {
        PhaseRelease rel = new PhaseRelease(id, dir, null);
        linkCoordinator.send(new Message<>("PHASE_RELEASE", rel));
    }

    /** Encaminhamento real */
    private void forwardVehicle(Vehicle v) {
        List<NodeId> path = v.getPath();
        int index = v.getPathIndex();

        if (index + 1 >= path.size()) {
            EventLogEntry ev = new EventLogEntry(
                    "VEHICLE_EXIT",
                    simClock.getSimTime(),
                    id,
                    "Vehicle=" + v.getId()
            );
            linkCoordinator.send(new Message<>("EVENT_LOG", ev));
            return;
        }

        NodeId next = path.get(index + 1);

        VehicleTransfer vt = new VehicleTransfer(
                v.getId(),
                id,
                next.name(),
                simClock.getSimTime(),
                path,
                index + 1,
                v.getType()
        );

        linkCoordinator.send(new Message<>("VehicleTransfer", vt));
    }

    /** Loop de telemetria */
    private void telemetryLoop() {
        while (true) {
            try {
                TelemetryPayload tp = new TelemetryPayload();
                tp.setCrossing(id);

                int total = 0;
                for (VehicleQueue q : queues.values()) total += q.size();
                tp.setQueue(total);

                tp.setAvg(0.0);
                tp.setLightState(currentLight);

                linkCoordinator.send(new Message<>("TELEMETRY", tp));

                Thread.sleep(telemetryIntervalMs);

            } catch (Exception ignored) {}
        }
    }

    /** Listener passivo (PolicyUpdate no futuro) */
    private void listenCoordinator() {
        try {
            while (true) {
                String line = linkCoordinator.receive();
                if (line == null) break;
                System.out.println("[Coordinator→" + id + "] " + line);
            }
        } catch (Exception e) {
            System.err.println("[Crossing " + id + "] Erro leitura Coordinator");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: CrossingProcess <ID> <PORT>");
            return;
        }
        new CrossingProcess(args[0], Integer.parseInt(args[1])).start();
    }
}
