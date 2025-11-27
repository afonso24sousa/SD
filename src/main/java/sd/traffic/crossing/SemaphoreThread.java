
package sd.traffic.crossing;

import sd.traffic.common.LinkIO;
import sd.traffic.common.Message;
import sd.traffic.common.Vehicle;
import sd.traffic.coordinator.models.PhaseRequest;
import sd.traffic.coordinator.models.PhaseRelease;
import sd.traffic.coordinator.models.TelemetryPayload;
import sd.traffic.model.LightColor;

/**
 * SemaphoreThread: representa um semáforo (direção) como uma thread.
 * Controla luz verde/vermelha, processa veículos e envia telemetria.
 */
public class SemaphoreThread extends Thread {
    private final String crossingId;
    private final String direction;
    private final QueueManager queueManager;
    private final LinkIO linkCoordinator;
    private volatile LightColor state = LightColor.RED;
    private final Object lock;
    private final long tSemMillis;

    public SemaphoreThread(String crossingId, String direction, QueueManager queueManager,
                           LinkIO linkCoordinator, Object lock, long tSemMillis) {
        this.crossingId = crossingId;
        this.direction = direction;
        this.queueManager = queueManager;
        this.linkCoordinator = linkCoordinator;
        this.lock = lock;
        this.tSemMillis = tSemMillis;
        setName("Semaphore-" + crossingId + "-" + direction);
    }

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
                    // Espera até ser notificado para ficar verde
                    while (state != LightColor.GREEN) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Quando está verde, processa veículos da fila
            while (state == LightColor.GREEN && queueManager.size() > 0) {
                Vehicle v = queueManager.dequeue();
                if (v != null) {
                    System.out.println("[" + crossingId + "] Direção " + direction + " processando veículo " + v.getId());
                    try {
                        Thread.sleep(tSemMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // Após esvaziar ou mudar estado, envia telemetria
            sendTelemetry();
        }
    }

    /** Ativa o semáforo (verde) e envia PhaseRequest */
    public void turnGreen() {
        synchronized (lock) {
            state = LightColor.GREEN;
            lock.notifyAll();
        }
        PhaseRequest req = new PhaseRequest(crossingId, direction, System.currentTimeMillis());
        linkCoordinator.send(new Message<>("PhaseRequest", req));
    }

    /** Desativa o semáforo (vermelho) e envia PhaseRelease */
    public void turnRed() {
        state = LightColor.RED;
        PhaseRelease rel = new PhaseRelease(crossingId, direction, System.currentTimeMillis());
        linkCoordinator.send(new Message<>("PhaseRelease", rel));
    }

    /** Envia telemetria para o Coordinator */
    private void sendTelemetry() {
        TelemetryPayload tel = new TelemetryPayload();
        tel.setCrossing(crossingId);
        tel.setQueue(queueManager.size());
        tel.setLightState(state);
        linkCoordinator.send(new Message<>("TELEMETRY", tel));
    }
}
