package sd.traffic.entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import sd.traffic.common.ConfigLoader;
import sd.traffic.common.NodeId;

import java.util.*;

/**
 * RouteSelector
 *
 * Responsável por:
 *  - Ler o ficheiro routes.json
 *  - Representar as rotas possíveis
 *  - Selecionar rota probabilística / shortest / fixed
 *
 * Melhoria incluída:
 *  - 3.1 Proteção contra totalProb == 0
 *  - 3.3 Constante ROUTES_PATH
 */
public class RouteSelector {

    /** Caminho para o ficheiro JSON (melhoria 3.3) */
    private static final String ROUTES_PATH = "src/main/resources/config/routes.json";

    public enum Strategy {
        RANDOM,
        SHORTEST,
        FIXED
    }

    public static class Route {
        private final List<NodeId> path;
        private final int probability;

        public Route(List<NodeId> path, int probability) {
            this.path = path;
            this.probability = probability;
        }

        public List<NodeId> getPath() { return path; }
        public int getProbability() { return probability; }
        public int getLength() { return path.size(); }

        @Override
        public String toString() {
            return "Route{ path=" + path + ", probability=" + probability + " }";
        }
    }

    private final Map<String, List<Route>> routesMap = new HashMap<>();
    private Strategy strategy = Strategy.RANDOM;
    private final Random random = new Random();

    public RouteSelector() {
        try {
            loadRoutesFromJson();
        } catch (Exception e) {
            System.err.println("[RouteSelector] Erro ao carregar routes.json: " + e.getMessage());
            e.printStackTrace();
            createDefaultRoutes();
        }
        System.out.println("[RouteSelector] Rotas carregadas: " + routesMap.keySet());
    }

    /** Lê rotas do JSON */
    private void loadRoutesFromJson() {
        JsonObject root = ConfigLoader.load(ROUTES_PATH);

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String entryId = entry.getKey();
            JsonArray routesArray = entry.getValue().getAsJsonArray();

            List<Route> list = new ArrayList<>();

            for (JsonElement el : routesArray) {
                JsonObject routeObj = el.getAsJsonObject();

                List<NodeId> path = new ArrayList<>();
                for (JsonElement nodeEl : routeObj.getAsJsonArray("path")) {
                    // Assumimos JSON válido no projeto académico
                    path.add(NodeId.valueOf(nodeEl.getAsString()));
                }

                int prob = routeObj.get("probability").getAsInt();
                list.add(new Route(path, prob));
            }

            routesMap.put(entryId, list);
        }
    }

    /** Rotas default se JSON falhar */
    private void createDefaultRoutes() {
        List<Route> e1Routes = Arrays.asList(
                new Route(Arrays.asList(NodeId.E1, NodeId.Cr1, NodeId.Cr4, NodeId.Cr5, NodeId.S), 34),
                new Route(Arrays.asList(NodeId.E1, NodeId.Cr1, NodeId.Cr2, NodeId.Cr5, NodeId.S), 33),
                new Route(Arrays.asList(NodeId.E1, NodeId.Cr1, NodeId.Cr2, NodeId.Cr3, NodeId.S), 33)
        );
        routesMap.put("E1", e1Routes);

        List<Route> e2Routes = Arrays.asList(
                new Route(Arrays.asList(NodeId.E2, NodeId.Cr2, NodeId.Cr5, NodeId.S), 34),
                new Route(Arrays.asList(NodeId.E2, NodeId.Cr2, NodeId.Cr3, NodeId.S), 33),
                new Route(Arrays.asList(NodeId.E2, NodeId.Cr2, NodeId.Cr1, NodeId.Cr4, NodeId.Cr5, NodeId.S), 33)
        );
        routesMap.put("E2", e2Routes);

        List<Route> e3Routes = Arrays.asList(
                new Route(Arrays.asList(NodeId.E3, NodeId.Cr3, NodeId.S), 34),
                new Route(Arrays.asList(NodeId.E3, NodeId.Cr3, NodeId.Cr2, NodeId.Cr5, NodeId.S), 33),
                new Route(Arrays.asList(NodeId.E3, NodeId.Cr3, NodeId.Cr2, NodeId.Cr1, NodeId.Cr4, NodeId.Cr5, NodeId.S), 33)
        );
        routesMap.put("E3", e3Routes);

        System.out.println("[RouteSelector] A usar rotas default embutidas (fallback).");
    }

    /** Seleciona rota conforme estratégia */
    public List<NodeId> selectRoute(NodeId entryPoint) {
        List<Route> routes = routesMap.get(entryPoint.name());
        if (routes == null || routes.isEmpty()) {
            System.err.println("[RouteSelector] Nenhuma rota definida para " + entryPoint);
            return Collections.singletonList(entryPoint);
        }

        Route chosen;

        switch (strategy) {
            case SHORTEST: chosen = selectShortestRoute(routes); break;
            case FIXED:    chosen = routes.get(0); break;
            case RANDOM:
            default:
                chosen = selectRandomRoute(routes);
        }

        return chosen.getPath();
    }

    /** RANDOM com proteção totalProb == 0 (melhoria 3.1) */
    private Route selectRandomRoute(List<Route> routes) {
        int totalProb = routes.stream().mapToInt(Route::getProbability).sum();

        if (totalProb <= 0) {
            System.err.println("[RouteSelector] totalProb=0 → fallback para primeira rota");
            return routes.get(0);
        }

        int rndValue = random.nextInt(totalProb);
        int cumulative = 0;

        for (Route r : routes) {
            cumulative += r.getProbability();
            if (rndValue < cumulative) return r;
        }

        return routes.get(routes.size() - 1);
    }

    private Route selectShortestRoute(List<Route> routes) {
        return routes.stream()
                .min(Comparator.comparingInt(Route::getLength))
                .orElse(routes.get(0));
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
        System.out.println("[RouteSelector] Estratégia alterada para " + strategy);
    }

    public Map<String, List<Route>> getRoutesMap() {
        return Collections.unmodifiableMap(routesMap);
    }
}
