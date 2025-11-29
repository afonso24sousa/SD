package sd.traffic.entry;

import com.google.gson.Gson;
import sd.traffic.common.*;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.coordinator.models.VehicleTransfer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processo de entrada: gera veículos segundo taxa λ (Poisson)
 * e envia-os ao primeiro cruzamento.
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
    private final Gson gson = new Gson();
    private volatile boolean running = true;

    public EntryGeneratorProcess(String entryId, double lambda, SimClock clock) {
        this.entryId = entryId;
        this.entryNode = NodeId.valueOf(entryId);
        this.lambda = lambda;
        this.clock = clock;
        this.link = new LinkIO("localhost", 6000);
        this.selector = new RouteSelector();
    }

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

    private void loop() {
        while (running) {
            try {
                double interval = nextInterval();
                long sleep = clock.toRealMillis(interval);

                Thread.sleep(sleep);
                clock.advance(interval);

                Vehicle v = createVehicle();
                sendToFirstCrossing(v);

                System.out.println("[Entry " + entryId + "] Gerado " + v.getId() +
                        " rota=" + v.getPath());

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private double nextInterval() {
        double u = rnd.nextDouble();
        if (u < 1e-6) u = 1e-6;
        return -Math.log(u) / lambda;
    }

    private Vehicle createVehicle() {
        Vehicle v = new Vehicle();
        v.setId(entryId + "_" + counter.incrementAndGet());

        // tipo baseado em percentagens
        int r = rnd.nextInt(100);
        if (r < 25) v.setType(VehicleType.MOTA);
        else if (r < 85) v.setType(VehicleType.CARRO);
        else v.setType(VehicleType.CAMIAO);

        v.setEntry(entryNode);
        List<NodeId> path = selector.selectRoute(entryNode);
        v.setPath(path);

        v.setEnteredAtSimTime(clock.getSimTime());
        v.setPathIndex(0);
        return v;
    }

    private void sendToFirstCrossing(Vehicle v) {
        List<NodeId> path = v.getPath();

        if (path.size() < 2) {
            System.err.println("[Entry " + entryId + "] Path inválido " + v.getId());
            return;
        }

        NodeId next = path.get(1);

        VehicleTransfer vt = new VehicleTransfer(
                v.getId(),
                entryId,
                next.name(),
                clock.getSimTime(),
                v.getPath(),
                1,
                v.getType()
        );

        link.send(new Message<>("VehicleTransfer", vt));
    }

    public void stop() {
        running = false;
        link.close();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: EntryGeneratorProcess <ID> <LAMBDA>");
            return;
        }
        String id = args[0];
        double lambda = Double.parseDouble(args[1]);
        SimClock clock = new SimClock();
        EntryGeneratorProcess gen = new EntryGeneratorProcess(id, lambda, clock);
        gen.start();
    }
}
