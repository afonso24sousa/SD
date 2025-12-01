package sd.traffic.coordinator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * CoordinatorEventList — versão FINAL
 *
 * ✔ Guarda todos os eventos estruturados
 * ✔ RAW events vão sempre para o fim (lamport = -1)
 * ✔ Snapshot ordenado por Lamport
 * ✔ Pode exportar a qualquer momento
 * ✔ Thread-safe
 */
public class CoordinatorEventList {

    /** priority = menor timestamp primeiro */
    private final PriorityQueue<CoordinatorEvent> pq =
            new PriorityQueue<>();

    private final Gson gson =
            new GsonBuilder().setPrettyPrinting().create();

    /** Adiciona evento estruturado */
    public synchronized void addEvent(CoordinatorEvent e) {
        pq.offer(e);
    }

    /** Adiciona RAW event sempre no fim */
    public synchronized void addRawEvent(String line) {
        pq.offer(new CoordinatorEvent(
                -1,              // ← garante que fica NO FUNDO da ordenação
                "RAW",
                "SYSTEM",
                line
        ));
    }

    /** Snapshot ORDENADO */
    public synchronized List<CoordinatorEvent> snapshot() {
        List<CoordinatorEvent> list = new ArrayList<>(pq);
        list.sort(null);                // usa compareTo real do CoordinatorEvent
        return list;
    }

    /** Exporta tudo para JSON */
    public synchronized void exportToJson(String path) {
        try (FileWriter fw = new FileWriter(path)) {
            gson.toJson(snapshot(), fw);
            System.out.println("[CoordinatorEventList] Exportado para: " + path);
        } catch (IOException e) {
            System.err.println("[CoordinatorEventList] Erro a exportar: " + e.getMessage());
        }
    }
}
