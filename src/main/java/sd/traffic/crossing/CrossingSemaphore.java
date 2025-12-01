package sd.traffic.crossing;

import sd.traffic.common.Message;
import sd.traffic.common.SimClock;
import sd.traffic.common.Vehicle;
import sd.traffic.coordinator.models.EventLogEntry;
import sd.traffic.coordinator.models.Policy;

/**
 * CrossingSemaphore FINAL – versão alinhada com PhaseController GLOBAL.
 *
 * Cada instância controla um único semáforo (N/S/E/W) num cruzamento.
 * Processo:
 *  - Espera por veículo
 *  - Pede GREEN global ao Coordinator
 *  - Processa veículos: MIN_GREEN → EXTENSIONS
 *  - YELLOW → CLEARANCE
 *  - Liberta GREEN no PhaseController
 *
 * O atributo currentlyProcessing permite ao CrossingProcess saber
 * se algum semáforo está ativo (usado para inferir estados no dashboard).
 */
public class CrossingSemaphore extends Thread {

    private final String crossingId;
    private final String direction;
    private final VehicleQueue queue;
    private final CrossingProcess crossingProcess;
    private final SimClock simClock;
    private final double tSem;

    private volatile boolean running = true;

    private int maxQueueSize = 0;
    private long totalProcessed = 0;
    private boolean warnedNoPolicy = false;

    /** Indica se este semáforo está a processar um veículo neste instante */
    private volatile boolean currentlyProcessing = false;

    public boolean isProcessing() {
        return currentlyProcessing;
    }

    public CrossingSemaphore(String crossingId,
                             String direction,
                             VehicleQueue queue,
                             CrossingProcess process,
                             SimClock simClock,
                             double tSem) {

        this.crossingId = crossingId;
        this.direction = direction;
        this.queue = queue;
        this.crossingProcess = process;
        this.simClock = simClock;
        this.tSem = tSem;

        setName("SEM-" + crossingId + "-" + direction);
    }

    @Override
    public void run() {

        while (running) {

            try {
                // 0) Esperar veículo disponível
                Vehicle first = queue.poll();
                if (first == null) {
                    Thread.sleep(30);
                    continue;
                }

                maxQueueSize = Math.max(maxQueueSize, queue.size());

                // 1) Obter política atual
                Policy policy = crossingProcess.getPolicy();
                if (policy == null) {
                    if (!warnedNoPolicy) {
                        System.err.println("[Semaphore " + crossingId + "-" + direction +
                                "] A aguardar política...");
                        warnedNoPolicy = true;
                    }
                    Thread.sleep(50);
                    continue;
                }
                warnedNoPolicy = false;

                int minGreen = policy.getMin_green();
                int maxGreen = policy.getMax_green();
                int qThreshold = policy.getQueue_threshold();
                int maxExt = policy.getMax_extensions();
                int yellow = policy.getYellow();
                int clearance = policy.getClearance();

                // 2) PHASE_REQUEST global
                crossingProcess.requestGreenGlobal(direction);

                EventLogEntry evStart = new EventLogEntry(
                        "GREEN_START",
                        simClock.getSimTime(),
                        crossingId,
                        null,
                        "direction=" + direction
                );
                crossingProcess.getCoordinatorLink()
                        .send(new Message<>("EVENT_LOG", evStart));

                double greenStart = simClock.getSimTime();
                double greenElapsed;
                int extensions = 0;

                // 3) MIN_GREEN → 1 veículo obrigatório
                processVehicle(first);

                greenElapsed = simClock.getSimTime() - greenStart;

                while (greenElapsed < minGreen) {
                    Vehicle v = queue.poll();
                    if (v == null) break;

                    processVehicle(v);
                    greenElapsed = simClock.getSimTime() - greenStart;
                }

                // 4) EXTENSIONS dinâmicas
                while (queue.size() >= qThreshold &&
                        extensions < maxExt &&
                        greenElapsed < maxGreen) {

                    Vehicle v = queue.poll();
                    if (v == null) break;

                    processVehicle(v);

                    extensions++;
                    greenElapsed = simClock.getSimTime() - greenStart;
                }

                // 5) YELLOW
                Thread.sleep(simClock.toRealMillis(yellow));
                simClock.advance(yellow);

                EventLogEntry evY = new EventLogEntry(
                        "YELLOW",
                        simClock.getSimTime(),
                        crossingId,
                        null,
                        "direction=" + direction
                );
                crossingProcess.getCoordinatorLink()
                        .send(new Message<>("EVENT_LOG", evY));

                // 6) CLEARANCE
                Thread.sleep(simClock.toRealMillis(clearance));
                simClock.advance(clearance);

                EventLogEntry evC = new EventLogEntry(
                        "CLEARANCE",
                        simClock.getSimTime(),
                        crossingId,
                        null,
                        "direction=" + direction
                );
                crossingProcess.getCoordinatorLink()
                        .send(new Message<>("EVENT_LOG", evC));

                // 7) PHASE_RELEASE global
                crossingProcess.releaseGreenGlobal(direction);

                EventLogEntry evEnd = new EventLogEntry(
                        "GREEN_END",
                        simClock.getSimTime(),
                        crossingId,
                        null,
                        "direction=" + direction + ", duration=" + greenElapsed
                );
                crossingProcess.getCoordinatorLink()
                        .send(new Message<>("EVENT_LOG", evEnd));

            } catch (InterruptedException e) {
                running = false;
            } catch (Exception e) {
                System.err.println("[Semaphore " + crossingId + "-" + direction +
                        "] ERRO: " + e.getMessage());
            }
        }
    }

    private void processVehicle(Vehicle v) throws InterruptedException {

        currentlyProcessing = true;

        double simDelta = tSem * v.getType().getFactor();

        Thread.sleep(simClock.toRealMillis(simDelta));
        simClock.advance(simDelta);

        crossingProcess.forwardVehicle(v);

        currentlyProcessing = false;

        totalProcessed++;
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
