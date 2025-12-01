package sd.traffic.crossing;

import sd.traffic.common.Vehicle;
import sd.traffic.common.VehicleType;

import java.util.LinkedList;

/**
 * VehicleQueue (versão completa)
 *
 * - Fila thread-safe com wait()/notifyAll()
 * - Mantém estatísticas obrigatórias:
 *      * tamanho atual
 *      * tamanho máximo
 *      * soma acumulada para média
 *      * total processados
 *      * contagem por tipo
 *
 * Estas estatísticas são necessárias para:
 *  - Telemetria do Dashboard
 *  - Políticas de controlo de semáforos
 *  - Estatísticas globais no Sink
 */
public class VehicleQueue {

    private final LinkedList<Vehicle> list = new LinkedList<>();

    /** Estatísticas */
    private int maxSize = 0;              // fila máxima observada
    private long totalEntries = 0;        // total de veículos enfileirados
    private long totalProcessed = 0;      // total de veículos retirados
    private long cumulativeSize = 0;      // soma dos tamanhos para média
    private long samplingCount = 0;       // número de amostras

    /** Contagem por tipo */
    private int motos = 0;
    private int carros = 0;
    private int camioes = 0;

    /** Enfileira um veículo */
    public synchronized void enqueue(Vehicle v) {
        list.addLast(v);

        totalEntries++;
        updateTypeCounter(v);

        int size = list.size();
        if (size > maxSize) maxSize = size;

        notifyAll();
    }

    /** Remove o próximo veículo; bloqueia se estiver vazia */
    public synchronized Vehicle poll() {
        while (list.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        Vehicle v = list.removeFirst();
        totalProcessed++;
        return v;
    }

    /** Regista uma amostragem para calcular média da fila */
    public synchronized void sample() {
        cumulativeSize += list.size();
        samplingCount++;
    }

    /** Retorna a média do tamanho da fila */
    public synchronized double getAverageSize() {
        if (samplingCount == 0) return 0.0;
        return (double) cumulativeSize / samplingCount;
    }

    /** Conta veículos por tipo */
    private void updateTypeCounter(Vehicle v) {
        if (v.getType() == VehicleType.MOTA) motos++;
        else if (v.getType() == VehicleType.CARRO) carros++;
        else if (v.getType() == VehicleType.CAMIAO) camioes++;
    }

    /** Getters thread-safe */
    public synchronized int size() { return list.size(); }

    public synchronized boolean isEmpty() { return list.isEmpty(); }

    public synchronized int getMaxSize() { return maxSize; }

    public synchronized long getTotalEntries() { return totalEntries; }

    public synchronized long getTotalProcessed() { return totalProcessed; }

    public synchronized int getMotos() { return motos; }

    public synchronized int getCarros() { return carros; }

    public synchronized int getCamioes() { return camioes; }

    @Override
    public synchronized String toString() {
        return "VehicleQueue{" +
                "current=" + list.size() +
                ", max=" + maxSize +
                ", avg=" + getAverageSize() +
                ", processed=" + totalProcessed +
                ", motos=" + motos +
                ", carros=" + carros +
                ", camioes=" + camioes +
                '}';
    }
}
