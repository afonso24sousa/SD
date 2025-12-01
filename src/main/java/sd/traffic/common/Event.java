package sd.traffic.common;

/**
 * Representa um evento discreto no sistema de simulação.
 *
 * Cada evento contém:
 *  - o instante de tempo (simulado),
 *  - o tipo do evento (ex.: "VEHICLE_ARRIVAL", "START_GREEN"),
 *  - um payload (dados adicionais, como um veículo ou cruzamento).
 *
 * A lista de eventos será gerida por uma PriorityQueue ordenada pelo tempo.
 * É a base do modelo de simulação por eventos discretos.
 */
public class Event implements Comparable<Event> {

    // Alguns tipos de evento comuns (ajuda a evitar strings "mágicas")
    public static final String VEHICLE_ENTRY       = "VEHICLE_ENTRY";
    public static final String VEHICLE_ARRIVAL     = "VEHICLE_ARRIVAL";
    public static final String VEHICLE_DEPARTURE   = "VEHICLE_DEPARTURE";
    public static final String LIGHT_GREEN         = "LIGHT_GREEN";
    public static final String LIGHT_YELLOW        = "LIGHT_YELLOW";
    public static final String LIGHT_RED           = "LIGHT_RED";

    /** Tempo simulado em que o evento ocorre */
    private final double time;

    /** Tipo do evento (string genérica para futura expansão) */
    private final String type;

    /** Dados adicionais associados ao evento (pode ser qualquer objeto) */
    private final Object payload;

    /**
     * Construtor principal.
     *
     * @param time Instante simulado do evento
     * @param type Tipo de evento (string)
     * @param payload Objeto associado ao evento (pode ser null)
     */
    public Event(double time, String type, Object payload) {
        this.time = time;
        this.type = type;
        this.payload = payload;
    }

    // Getters
    public double getTime() { return time; }
    public String getType() { return type; }
    public Object getPayload() { return payload; }

    /**
     * Permite ordenar eventos por tempo numa PriorityQueue
     * (eventos com menor time têm maior prioridade).
     */
    @Override
    public int compareTo(Event other) {
        return Double.compare(this.time, other.time);
    }

    @Override
    public String toString() {
        return "Event{" +
                "time=" + time +
                ", type='" + type + '\'' +
                '}';
    }
}
