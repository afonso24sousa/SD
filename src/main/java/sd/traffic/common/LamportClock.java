package sd.traffic.common;

/**
 * Implementação simples de um relógio lógico de Lamport.
 *
 * Pode ser usado para ordenar causalmente eventos entre processos
 * distribuídos. No projeto, é útil para marcar mensagens e logs.
 */
public class LamportClock {

    private long time = 0;

    /** Incrementa o relógio local (evento interno ou envio). */
    public synchronized long increment() {
        time++;
        return time;
    }

    /**
     * Atualiza o relógio aquando da receção de uma mensagem.
     *
     * @param receivedTime timestamp lógico recebido
     * @return novo valor do relógio local
     */
    public synchronized long update(long receivedTime) {
        time = Math.max(time, receivedTime) + 1;
        return time;
    }

    public synchronized long getTime() {
        return time;
    }

    /** Define explicitamente o valor do relógio (usado raramente). */
    public synchronized void setTime(long newTime) {
        this.time = newTime;
    }
}
