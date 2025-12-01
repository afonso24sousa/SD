package sd.traffic.coordinator;

import java.util.HashMap;
import java.util.Map;

/**
 * PhaseController FINAL — versão corrigida e alinhada com o enunciado.
 *  - Exclusão mútua entre NS, EW e PEDESTRIAN
 *  - Agora previne mistura entre GRUPOS e PEDESTRIAN
 *  - Removido bug "O"
 */
public class PhaseController {

    private final Map<String, String> currentPhase = new HashMap<>();

    private String groupOf(String direction) {
        switch (direction) {
            case "N":
            case "S":
                return "NS";
            case "E":
            case "W":
                return "EW";
            case "PEDESTRIAN":
                return "PEDESTRIAN";
            default:
                return direction;
        }
    }

    private boolean canTurnGreen(String crossingId, String direction) {
        String requested = groupOf(direction);
        String active = currentPhase.get(crossingId);

        if (active == null) return true;

        // Pedestres bloqueiam tudo
        if ("PEDESTRIAN".equals(active)) return false;

        // Se pedestres pedem mas carros estão ativos → NÃO pode
        if ("PEDESTRIAN".equals(requested) && !"PEDESTRIAN".equals(active))
            return false;

        return active.equals(requested);
    }

    public synchronized void requestGreen(String crossingId, String direction) {
        while (!canTurnGreen(crossingId, direction)) {
            try { wait(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        currentPhase.put(crossingId, groupOf(direction));
    }

    public synchronized void releaseGreen(String crossingId) {
        currentPhase.put(crossingId, null);
        notifyAll();
    }

    /** Pedestrian API */
    public synchronized void requestPedestrian(String crossingId) {
        while (currentPhase.get(crossingId) != null) {
            try { wait(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        currentPhase.put(crossingId, "PEDESTRIAN");
    }

    public synchronized void releasePedestrian(String crossingId) {
        currentPhase.put(crossingId, null);
        notifyAll();
    }

    public synchronized String getCurrentPhase(String crossingId) {
        return currentPhase.get(crossingId);
    }
}
