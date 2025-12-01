package sd.traffic.entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import sd.traffic.common.ConfigLoader;
import sd.traffic.common.NodeId;

import java.util.*;

/**
 * RouteSelector FINAL
 *
 * - Lê routes.json
 * - Mantém rotas com probabilidade
 * - Suporta estratégias: RANDOM, SHORTEST, FIXED
 */
public class RouteSelector {

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
            return "Route{path=" + path + ", prob=" + probability + "}";
        }
    }

    private final Map<String, List<Route>> routesMap = new HashMap<>();
    private Strategy strategy = Strategy.RANDOM;
    private final Random random = new Random();

    public RouteSelector() {
        try {
            loadRoutesFromJson();
        } catch (Exception e) {
            System.err.println("[RouteSelector] Erro ao ler routes.json: " + e.getMessage());
            createDefaultRoutes();
        }
        System.out.println("[RouteSelector] Rotas carregadas: " + routesMap.keySet());
    }

    private void loadRoutesFromJson() {
        JsonObject root = ConfigLoader.load(ROUTES_PATH);

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String entryId = entry.getKey();
            JsonArray arr = entry.getValue().getAsJsonArray();

            List<Route> list = new ArrayList<>();

            for (JsonElement e : arr) {
                JsonObject routeObj = e.getAsJsonObject();

                List<NodeId> path = new ArrayList<>();
                for (JsonElement nodeEl : routeObj.getAsJsonArray("path")) {
                    path.add(NodeId.valueOf(nodeEl.getAsString()));
                }

                int prob = routeObj.get("probability").getAsInt();
                list.add(new Route(path, prob));
            }

            routesMap.put(entryId, list);
        }
    }

    private void createDefaultRoutes() {
        routesMap.put("E1", Arrays.asList(
                new Route(Arrays.asList(NodeId.E1, NodeId.Cr1, NodeId.Cr4, NodeId.Cr5, NodeId.S), 34),
                new Route(Arrays.asList(NodeId.E1, NodeId.Cr1, NodeId.Cr2, NodeId.Cr5, NodeId.S), 33),
                new Route(Arrays.asList(NodeId.E1, NodeId.Cr1, NodeId.Cr2, NodeId.Cr3, NodeId.S), 33)
        ));

        routesMap.put("E2", Arrays.asList(
                new Route(Arrays.asList(NodeId.E2, NodeId.Cr2, NodeId.Cr5, NodeId.S), 34),
                new Route(Arrays.asList(NodeId.E2, NodeId.Cr2, NodeId.Cr3, NodeId.S), 33),
                new Route(Arrays.asList(NodeId.E2, NodeId.Cr2, NodeId.Cr1, NodeId.Cr4, NodeId.Cr5, NodeId.S), 33)
        ));

        routesMap.put("E3", Arrays.asList(
                new Route(Arrays.asList(NodeId.E3, NodeId.Cr3, NodeId.S), 34),
                new Route(Arrays.asList(NodeId.E3, NodeId.Cr3, NodeId.Cr2, NodeId.Cr5, NodeId.S), 33),
                new Route(Arrays.asList(NodeId.E3, NodeId.Cr3, NodeId.Cr2, NodeId.Cr1, NodeId.Cr4, NodeId.Cr5, NodeId.S), 33)
        ));
    }

    public List<NodeId> selectRoute(NodeId entryPoint) {
        List<Route> routes = routesMap.get(entryPoint.name());
        if (routes == null || routes.isEmpty()) {
            System.err.println("[RouteSelector] Nenhuma rota definida para " + entryPoint);
            return Collections.singletonList(entryPoint);
        }

        Route chosen;

        switch (strategy) {
            case SHORTEST:
                chosen = routes.stream()
                        .min(Comparator.comparingInt(Route::getLength))
                        .orElse(routes.get(0));
                break;

            case FIXED:
                chosen = routes.get(0);
                break;

            case RANDOM:
            default:
                chosen = selectRandomRoute(routes);
        }

        return chosen.getPath();
    }

    private Route selectRandomRoute(List<Route> routes) {
        int totalProb = routes.stream().mapToInt(Route::getProbability).sum();

        if (totalProb <= 0) {
            System.err.println("[RouteSelector] totalProb=0 → fallback first route");
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

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
        System.out.println("[RouteSelector] Estratégia definida: " + strategy);
    }

    public Map<String, List<Route>> getRoutesMap() {
        return Collections.unmodifiableMap(routesMap);
    }
}
