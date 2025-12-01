package sd.traffic.sink;

import sd.traffic.common.VehicleType;
import java.util.HashMap;
import java.util.Map;

/**
 * SinkSummary FINAL — versão robusta, completa e alinhada com SinkProcess.
 *
 * ✔ Estatísticas completas por tipo
 * ✔ Estatísticas globais
 * ✔ Suporta consolidação final (finalizeAll)
 * ✔ Seguro quando não há veículos de um tipo
 * ✔ 100% serializável para enviar ao Dashboard
 */
public class SinkSummary {

    // ============================================================
    //  ESTATÍSTICAS POR TIPO
    // ============================================================

    public static class Stats {

        private long count = 0;
        private double min = 0.0;
        private double max = 0.0;
        private double sum = 0.0;   // média calculada com sum/count
        private double avg = 0.0;

        public void add(double value) {
            if (count == 0) {
                min = value;
                max = value;
            } else {
                if (value < min) min = value;
                if (value > max) max = value;
            }
            count++;
            sum += value;
        }

        public void finalizeAvg() {
            if (count > 0) {
                avg = sum / count;
            } else {
                min = 0;
                max = 0;
                avg = 0;
            }
        }

        public long getCount() { return count; }
        public double getMin()  { return min; }
        public double getMax()  { return max; }
        public double getAvg()  { return avg; }
    }

    // ============================================================
    //  CAMPOS PRINCIPAIS
    // ============================================================

    private double totalSimTime;

    private final Stats motaStats   = new Stats();
    private final Stats carroStats  = new Stats();
    private final Stats camiaoStats = new Stats();

    // Estatísticas globais — úteis para relatório
    private long totalVehicles = 0;
    private double globalMin = 0;
    private double globalMax = 0;
    private double globalAvg = 0;

    // ============================================================
    //  ACESSO POR VehicleType
    // ============================================================

    public Stats getStatsFor(VehicleType t) {
        switch (t) {
            case MOTA:   return motaStats;
            case CARRO:  return carroStats;
            case CAMIAO: return camiaoStats;
            default:     return carroStats;
        }
    }

    // ============================================================
    //  CONSOLIDAÇÃO FINAL (antes de enviar)
    // ============================================================

    public void finalizeAll() {

        motaStats.finalizeAvg();
        carroStats.finalizeAvg();
        camiaoStats.finalizeAvg();

        // Total veículos
        totalVehicles =
                motaStats.getCount() +
                        carroStats.getCount() +
                        camiaoStats.getCount();

        // Cálculo global
        globalMin = minNonZero(
                motaStats.getMin(),
                carroStats.getMin(),
                camiaoStats.getMin()
        );

        globalMax = Math.max(
                motaStats.getMax(),
                Math.max(carroStats.getMax(), camiaoStats.getMax())
        );

        globalAvg = weightedAverage();
    }

    private double weightedAverage() {
        double num =
                motaStats.getAvg() * motaStats.getCount() +
                        carroStats.getAvg() * carroStats.getCount() +
                        camiaoStats.getAvg() * camiaoStats.getCount();

        return totalVehicles == 0 ? 0.0 : num / totalVehicles;
    }

    private double minNonZero(double... values) {
        double best = Double.MAX_VALUE;
        for (double v : values) {
            if (v > 0 && v < best) best = v;
        }
        return best == Double.MAX_VALUE ? 0 : best;
    }

    // ============================================================
    //  GETTERS / SETTERS
    // ============================================================

    public double getTotalSimTime() { return totalSimTime; }
    public void setTotalSimTime(double totalSimTime) { this.totalSimTime = totalSimTime; }

    public Stats getMotaStats()   { return motaStats; }
    public Stats getCarroStats()  { return carroStats; }
    public Stats getCamiaoStats() { return camiaoStats; }

    public long getTotalVehicles() { return totalVehicles; }
    public double getGlobalMin()   { return globalMin; }
    public double getGlobalMax()   { return globalMax; }
    public double getGlobalAvg()   { return globalAvg; }
}
