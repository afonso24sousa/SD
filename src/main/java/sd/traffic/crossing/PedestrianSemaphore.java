package sd.traffic.crossing;

import sd.traffic.common.Message;
import sd.traffic.common.SimClock;
import sd.traffic.coordinator.models.EventLogEntry;
import sd.traffic.coordinator.models.Policy;

/**
 * PedestrianSemaphore FINAL — REVISADO
 * ------------------------------------
 * - Peões têm ciclo independente (apenas em Cr4)
 * - Usa PhaseController GLOBAL (PEDESTRIAN é grupo exclusivo)
 * - Nunca interrompe carros a meio da fase verde
 * - Não entra em conflito com policy (espera se policy ainda não chegou)
 * - EVENT_LOG completo e compatível com CoordinatorServer
 * - Evita intercalar pedestrian cycles demasiado rápido
 */
public class PedestrianSemaphore extends Thread {

    private final String crossingId;
    private final CrossingProcess crossing;
    private final SimClock simClock;

    private final double tPedGreen;      // tempo simulado de verde pedestre
    private final long cycleIntervalMs;  // tempo real entre ciclos

    private volatile boolean running = true;

    public PedestrianSemaphore(
            String crossingId,
            CrossingProcess crossingProcess,
            SimClock simClock,
            double tPedGreen,
            long cycleIntervalMs
    ) {
        this.crossingId = crossingId;
        this.crossing = crossingProcess;
        this.simClock = simClock;
        this.tPedGreen = tPedGreen;
        this.cycleIntervalMs = cycleIntervalMs;

        setName("PedestrianSemaphore-" + crossingId);
    }

    @Override
    public void run() {

        while (running) {
            try {
                // ============================================================
                // 0) Esperar ciclo natural (porque peões não podem acontecer
                //    sempre que quiserem — é um ciclo periódico)
                // ============================================================
                Thread.sleep(cycleIntervalMs);

                // ============================================================
                // 1) Esperar que chegue a política (importante!)
                // ============================================================
                while (crossing.getPolicy() == null) {
                    Thread.sleep(80);
                }

                Policy policy = crossing.getPolicy();

                // Opcional: Se a policy incluir algum parâmetro especial
                // para peões no futuro, esta é a zona ideal para integrá-lo.

                // ============================================================
                // 2) Esperar que NENHUM carro esteja em verde
                //    (fila realista: nunca interromper um verde automóvel)
                // ============================================================
                while (crossing.isCarPhaseActive()) {
                    Thread.sleep(80);
                }

                // ============================================================
                // 3) Pedir fase PEDESTRIAN ao Coordinator
                // ============================================================
                crossing.requestPedestrianGreenGlobal();

                // EVENT_LOG: pedestres ganharam verde
                EventLogEntry evStart = new EventLogEntry(
                        "PEDESTRIAN_GREEN",
                        simClock.getSimTime(),
                        crossingId,
                        null,
                        "Peões iniciam travessia"
                );
                crossing.getCoordinatorLink().send(new Message<>("EVENT_LOG", evStart));

                // ============================================================
                // 4) Tempo simulado de travessia dos peões
                // ============================================================
                long realMs = simClock.toRealMillis(tPedGreen);
                Thread.sleep(realMs);
                simClock.advance(tPedGreen);

                // ============================================================
                // 5) Libertar fase pedestre
                // ============================================================
                crossing.releasePedestrianGreenGlobal();

                // EVENT_LOG: pedestres ficaram vermelhos
                EventLogEntry evEnd = new EventLogEntry(
                        "PEDESTRIAN_RED",
                        simClock.getSimTime(),
                        crossingId,
                        null,
                        "Peões terminaram travessia"
                );
                crossing.getCoordinatorLink().send(new Message<>("EVENT_LOG", evEnd));

                // ============================================================
                // 6) Pausa de segurança antes de permitir novo pedido
                //    (evita spam de pedidos seguidos antes dos carros atuarem)
                // ============================================================
                Thread.sleep(150);

            } catch (InterruptedException ie) {
                running = false;
                Thread.currentThread().interrupt();

            } catch (Exception e) {
                System.err.println("[PedestrianSemaphore " + crossingId + "] ERRO: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
