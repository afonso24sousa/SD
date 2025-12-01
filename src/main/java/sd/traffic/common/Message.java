package sd.traffic.common;

/**
 * Classe genérica para representar mensagens trocadas entre processos.
 *
 * Todas as comunicações entre processos (Coordinator, Crossings, Dashboard, etc.)
 * são encapsuladas em objetos Message, que contêm:
 *  - um tipo de mensagem (string),
 *  - e um payload genérico (dados em JSON ou objeto Java).
 *
 * Exemplo:
 *   new Message<>("Telemetry", "{crossing:'Cr1',queue:4}");
 *   new Message<>("VehicleTransfer", vehicleObject);
 *
 * O tipo genérico permite que o payload seja qualquer tipo de objeto.
 */
public class Message<T> {

    /** Tipo da mensagem — indica o seu propósito */
    private String type;

    /** Conteúdo da mensagem — pode ser JSON, objeto Vehicle, ou estrutura Telemetry */
    private T payload;

    /** Construtor vazio exigido pelo Gson */
    public Message() {
    }

    /**
     * Construtor principal.
     *
     * @param type    Tipo da mensagem
     * @param payload Dados associados à mensagem
     */
    public Message(String type, T payload) {
        this.type = type;
        this.payload = payload;
    }

    // Getters e setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", payload=" + payload +
                '}';
    }
}
