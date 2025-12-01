package sd.traffic.common;

/**
 * Classe responsável por gerir o tempo simulado do sistema.
 *
 * Esta classe implementa um relógio lógico para a simulação de tráfego.
 * Permite converter tempo simulado em tempo real e controlar a progressão
 * dos eventos. É usada em todas as fases do projeto, especialmente no
 * Coordinator e nos CrossingProcess.
 *
 * O tempo simulado avança manualmente (com {@link #advance(double)})
 * de acordo com o processamento dos eventos, garantindo que o sistema
 * é determinístico e reproduzível.
 */
public class SimClock {

    /** Tempo atual da simulação (em segundos simulados). */
    private double currentSimTime = 0.0;

    /** Escala de conversão entre tempo simulado e tempo real (1s sim = 50ms reais). */
    private double timeScale = 0.05;

    /**
     * Obtém o tempo simulado atual.
     * O método é synchronized para garantir consistência em ambiente multithread.
     *
     * @return tempo simulado atual em segundos
     */
    public synchronized double getSimTime() {
        return currentSimTime;
    }

    /**
     * Define explicitamente o tempo simulado atual.
     * Útil quando seguimos uma lista de eventos discretos.
     */
    public synchronized void setSimTime(double newTime) {
        this.currentSimTime = newTime;
    }

    /**
     * Avança o tempo simulado em um dado intervalo.
     * Usado para simular a passagem de tempo entre eventos.
     *
     * @param delta quantidade de tempo (em segundos simulados) a adicionar
     */
    public synchronized void advance(double delta) {
        currentSimTime += delta;
    }

    /**
     * Converte segundos simulados em milissegundos reais.
     * Permite definir delays reais equivalentes ao tempo simulado.
     *
     * @param simSeconds segundos simulados
     * @return milissegundos reais correspondentes
     */
    public long toRealMillis(double simSeconds) {
        return (long) (simSeconds * 1000 * timeScale);
    }

    /** Obtém a escala de tempo atual. */
    public double getTimeScale() {
        return timeScale;
    }

    /** Define uma nova escala de tempo. */
    public void setTimeScale(double timeScale) {
        this.timeScale = timeScale;
    }

    /** Reinicia o relógio para 0 (útil em testes ou novos cenários). */
    public synchronized void reset() {
        this.currentSimTime = 0.0;
    }
}
