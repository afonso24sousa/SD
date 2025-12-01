package sd.traffic.coordinator.models;

import java.util.Map;

/**
 * Estatísticas globais enviadas pelo Sink para o Coordinator.
 * Este payload será enviado ao dashboard.
 */
public class TrafficStatsPayload {

    private int totalVehicles;

    private Map<String, Integer> countByType;
    private Map<String, Double> avgDwellingByType;
    private Map<String, Double> minDwellingByType;
    private Map<String, Double> maxDwellingByType;

    public int getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(int totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public Map<String, Integer> getCountByType() {
        return countByType;
    }

    public void setCountByType(Map<String, Integer> countByType) {
        this.countByType = countByType;
    }

    public Map<String, Double> getAvgDwellingByType() {
        return avgDwellingByType;
    }

    public void setAvgDwellingByType(Map<String, Double> avgDwellingByType) {
        this.avgDwellingByType = avgDwellingByType;
    }

    public Map<String, Double> getMinDwellingByType() {
        return minDwellingByType;
    }

    public void setMinDwellingByType(Map<String, Double> minDwellingByType) {
        this.minDwellingByType = minDwellingByType;
    }

    public Map<String, Double> getMaxDwellingByType() {
        return maxDwellingByType;
    }

    public void setMaxDwellingByType(Map<String, Double> maxDwellingByType) {
        this.maxDwellingByType = maxDwellingByType;
    }
}
