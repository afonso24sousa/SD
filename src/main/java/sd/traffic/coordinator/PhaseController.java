package sd.traffic.coordinator;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsável por coordenar quem pode ter semáforo verde.
 * Fase 3 — exclusão mútua entre cruzamentos/direções.
 */
public class PhaseController {

    // Guarda qual direção está com o verde em cada cruzamento
    private final Map<String, String> currentPhase = new HashMap<>();

    private boolean canTurnGreen(String crossingId, String direction){
        String current = currentPhase.get(crossingId);

        if (current==null) return true;

        return (current.equals(direction));

    }

    public synchronized void requestGreen(String crossingId, String direction){
        while (!canTurnGreen(crossingId, direction)){
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        currentPhase.put(crossingId, direction);
    }

    public synchronized void releaseGreen(String crossingId){
        currentPhase.put(crossingId, null);
        notifyAll();
    }

    public synchronized String getCurrentPhase(String crossingId) {
        return currentPhase.get(crossingId);
    }

}
