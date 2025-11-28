package sd.traffic.crossing;

import sd.traffic.common.Vehicle;
import java.util.LinkedList;

public class VehicleQueue {

    private final LinkedList<Vehicle> list = new LinkedList<>();

    /** Enfileira um veículo (como addLast) */
    public synchronized void enqueue(Vehicle v) {
        list.addLast(v);
        notifyAll();
    }

    /** Remove o próximo veículo; bloqueia com wait() se estiver vazia */
    public synchronized Vehicle poll() {
        while (list.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return list.removeFirst();
    }

    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized int size() {
        return list.size();
    }
}

