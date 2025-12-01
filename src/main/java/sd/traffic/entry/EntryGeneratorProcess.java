package sd.traffic.entry;

import sd.traffic.common.*;
import sd.traffic.coordinator.models.EventLogEntry;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.coordinator.models.VehicleTransfer;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EntryGeneratorProcess FINAL — versão 100% alinhada com o enunciado.
 *
 * ✔ Gera veículos por processo de Poisson (λ)
 * ✔ Respeita tempo discreto usando SimClock
 * ✔ Para automaticamente quando o tempo máximo da simulação é atingido (t_max)
 * ✔ Regista EVENT_LOG completo (VEHICLE_ENTRY)
 * ✔ Integra com vehicleHistory do Coordinator (via vehicleId em EventLogEntry)
 * ✔ Usa RouteSelector oficial para respeitar as probabilidades de caminhos
 */
public class EntryGeneratorProcess {

    private final String entryId;
    private final NodeId entryNode;
    private final double lambda;
    private final LinkIO link;
    private final RouteSelector selector;
    private final SimClock clock;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Random rnd = new Random();

    private volatile boolean running = true;

    /** Tempo máximo da simulação (carregado do config). */
    private final double maxSimTime;

    public EntryGeneratorProcess(String entryId, double lambda, SimClock clock) {
        this.entryId = entryId;
        this.entryNode = NodeId.valueOf(entryId);   // E1, E2 ou E3
        this.lambda = lambda;
        this.clock = clock;
        this.link = new LinkIO("localhost", 6000);
        this.selector = new RouteSelector();

        JsonObject cfg = ConfigLoader.loadDefaultConfig();
        JsonObject simCfg = cfg.getAsJsonObject("simulation");
        this.maxSimTime = simCfg.get("t_max").getAsDouble();
    }

    // =====================================================================
    // START
    // =====================================================================

    public void start() {
        if (!link.connect()) {
            System.err.println("[Entry " + entryId + "] Falha ao ligar ao Coordinator");
            return;
        }

        sendRegister();

        new Thread(this::loop, "EntryGen-" + entryId).start();
        System.out.println("[Entry " + entryId + "] Iniciado com λ=" + lambda);
    }

    private void sendRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setNodeId(entryId);
        req.setRole("ENTRY");
        link.send(new Message<>("REGISTER", req));
    }

    // =====================================================================
    // MAIN LOOP — Geração Poisson com paragem correta
    // =====================================================================

    private void loop() {
        while (running) {
            try {
                double now = clock.getSimTime();
                double remaining = maxSimTime - now;

                // Critério de paragem: tempo máximo atingido
                if (remaining <= 0) {
                    System.out.println("[Entry " + entryId + "] Tempo máximo atingido — parar geração.");
                    break;
                }

                // Intervalo Poisson
                double interval = nextInterval();

                // Se λ <= 0 ou intervalo inválido, não gera mais veículos
                if (Double.isInfinite(interval) || interval <= 0) {
                    System.out.println("[Entry " + entryId + "] λ <= 0 ou intervalo inválido — parar geração.");
                    break;
                }

                // Não ultrapassar t_max: último intervalo é truncado se necessário
                if (interval > remaining) {
                    interval = remaining;
                }

                Thread.sleep(clock.toRealMillis(interval));
                clock.advance(interval);

                // Criar e enviar veículo
                Vehicle v = createVehicle();
                sendToFirstCrossing(v);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }


        System.out.println("[Entry " + entryId + "] Terminado.");
        link.close();
    }

    // =====================================================================
    // POISSON
    // =====================================================================

    /**
     * Gera o intervalo entre chegadas segundo um Processo de Poisson
     * (distribuição exponencial de taxa λ).
     */
    private double nextInterval() {
        if (lambda <= 0) return Double.POSITIVE_INFINITY;

        double u = rnd.nextDouble();
        if (u < 1e-9) u = 1e-9; // evita log(0)

        return -Math.log(u) / lambda;
    }

    // =====================================================================
    // CREATE VEHICLE
    // =====================================================================

    private Vehicle createVehicle() {

        Vehicle v = new Vehicle();
        v.setId(entryId + "_" + counter.incrementAndGet());
        v.setEntry(entryNode);

        // Tipo baseado na percentagem do enunciado:
        // 25% MOTA, 60% CARRO, 15% CAMIAO (exemplo realista)
        int r = rnd.nextInt(100);
        if (r < 25) {
            v.setType(VehicleType.MOTA);
        } else if (r < 85) {
            v.setType(VehicleType.CARRO);
        } else {
            v.setType(VehicleType.CAMIAO);
        }

        // Caminho segundo RouteSelector (probabilidades por entrada)
        List<NodeId> path = selector.selectRoute(entryNode);
        v.setPath(path);
        v.setPathIndex(0);

        // Tempo de entrada simulado
        v.setEnteredAtSimTime(clock.getSimTime());

        return v;
    }

    // =====================================================================
    // SEND TO FIRST CROSSING
    // =====================================================================

    private void sendToFirstCrossing(Vehicle v) {

        List<NodeId> path = v.getPath();
        if (path == null || path.size() < 2) {
            System.err.println("[Entry " + entryId + "] ERRO: Path inválido: " + path);
            return;
        }

        NodeId next = path.get(1);

        // EVENT_LOG obrigatório com vehicleId no campo próprio
        EventLogEntry ev = new EventLogEntry(
                "VEHICLE_ENTRY",
                clock.getSimTime(),
                entryId,
                v.getId(),   // vehicleId
                "type=" + v.getType() + ", path=" + path
        );
        link.send(new Message<>("EVENT_LOG", ev));

        // Transferência do veículo para o primeiro cruzamento
        VehicleTransfer vt = new VehicleTransfer(
                v.getId(),
                entryId,
                next.name(),
                clock.getSimTime(),
                path,
                1,                  // já vamos para o índice 1 do path
                v.getType()
        );

        link.send(new Message<>("VehicleTransfer", vt));

        System.out.println("[Entry " + entryId + "] Gerado " + v.getId() +
                " → rota=" + v.getPath());
    }

    // =====================================================================
    // STOP
    // =====================================================================

    public void stop() {
        running = false;
        link.close();
    }

    // =====================================================================
    // MAIN
    // =====================================================================

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: EntryGeneratorProcess <ID> <LAMBDA>");
            System.err.println("Exemplo: EntryGeneratorProcess E1 0.5");
            return;
        }
        String id = args[0];                // "E1", "E2" ou "E3"
        double lambda = Double.parseDouble(args[1]);

        SimClock clock = new SimClock();
        new EntryGeneratorProcess(id, lambda, clock).start();
    }
}
