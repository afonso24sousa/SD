package sd.traffic.coordinator;

import com.google.gson.Gson;
import sd.traffic.coordinator.models.Policy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PolicyManager {

    private static final String POLICY_PATH = "src/main/resources/config/policy_hybrid.json";

    private final Gson gson = new Gson();
    private Policy currentPolicy = new Policy();

    public PolicyManager() {
        reload();
    }

    /** Recarrega política do ficheiro */
    public synchronized void reload() {
        try {
            String json = Files.readString(Paths.get(POLICY_PATH), StandardCharsets.UTF_8);
            currentPolicy = gson.fromJson(json, Policy.class);
            System.out.println("[PolicyManager] Loaded: " + currentPolicy);

        } catch (Exception e) {
            System.err.println("[PolicyManager] Erro ao ler política: " + e.getMessage());
        }
    }

    /** Retorna objeto Policy já parseado */
    public synchronized Policy getPolicy() {
        return currentPolicy;
    }

    /** Retorna JSON bruto (se precisares) */
    public synchronized String getPolicyJson() {
        return gson.toJson(currentPolicy);
    }
}
