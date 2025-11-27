package sd.traffic.crossing;

import sd.traffic.common.Vehicle;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * QueueManager: gere a fila de veículos para uma direção específica.
 * Usa ConcurrentLinkedQueue para garantir thread-safety.
 */
public class QueueManager {
    private final ConcurrentLinkedQueue<Vehicle> queue = new ConcurrentLinkedQueue<>();

    /** Adiciona um veículo à fila */
    public void enqueue(Vehicle v) {
        queue.add(v);
    }

    /** Remove e retorna o primeiro veículo da fila (ou null se vazia) */
    public Vehicle dequeue() {
        return queue.poll();
    }

    /** Retorna o tamanho atual da fila */
    public int size() {
        return queue.size();
    }
}

