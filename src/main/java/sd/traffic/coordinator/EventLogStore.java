package sd.traffic.coordinator;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Store simples de eventos:
 *  - Guarda linha JSON por linha
 *  - Thread-safe
 *  - Cria diretórios automaticamente
 *  - Permite leitura total para o Sink
 */
public class EventLogStore {

    private final String path;

    public EventLogStore(String path) {
        this.path = path;

        try {
            Path p = Paths.get(path).toAbsolutePath();
            Path dir = p.getParent();
            if (dir != null) Files.createDirectories(dir);
        } catch (IOException ignore) {}
    }

    /** Append thread-safe */
    public synchronized void append(String jsonLine) {
        if (jsonLine == null || jsonLine.trim().isEmpty()) return;

        try (FileWriter fw = new FileWriter(path, true)) {

            fw.write(jsonLine.trim());
            fw.write(System.lineSeparator());
            fw.flush();

        } catch (IOException e) {
            System.err.println("[EventLogStore] Erro a escrever em " + path + ": " + e.getMessage());
        }
    }

    /** Leitura integral (usado pelo Sink para reprocessar logs, se necessário) */
    public synchronized List<String> readAll() {
        try {
            return Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            System.err.println("[EventLogStore] Erro a ler ficheiro: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
