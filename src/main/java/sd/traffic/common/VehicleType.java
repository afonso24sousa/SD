package sd.traffic.common;

/**
 * Enumeração dos tipos de veículos presentes na simulação.
 *
 * Cada tipo possui:
 *  - um fator de velocidade relativo (para cálculo de t_road),
 *  - uma percentagem de ocorrência (para geração de tráfego),
 *  - e uma cor usada na visualização (dashboard).
 *
 * Exemplo: MOTA é 2x mais rápida que o carro, enquanto CAMIAO é 2x mais lento.
 */
public enum VehicleType {

    /** Motociclos — 50% do tempo de um carro (duas vezes mais rápidos). */
    MOTA(0.5, 25, "Azul"),

    /** Carros — referência base de velocidade e proporção. */
    CARRO(1.0, 60, "Verde"),

    /** Camiões — o dobro do tempo de um carro (metade da velocidade). */
    CAMIAO(2.0, 15, "Vermelho");

    /** Fator de tempo relativo (usado em t_road * fator). */
    private final double factor;

    /** Percentagem de ocorrência na geração de tráfego. */
    private final int percentage;

    /** Cor associada no dashboard. */
    private final String color;

    VehicleType(double factor, int percentage, String color) {
        this.factor = factor;
        this.percentage = percentage;
        this.color = color;
    }

    public double getFactor() { return factor; }
    public int getPercentage() { return percentage; }
    public String getColor() { return color; }

    /**
     * Utilitário: dada um número entre 0 e 99, devolve o tipo de veículo
     * correspondente às percentagens definidas.
     *
     * Exemplo:
     *   int r = random.nextInt(100);
     *   VehicleType type = VehicleType.fromRandomPercentage(r);
     */
    public static VehicleType fromRandomPercentage(int r) {
        int cumulative = 0;
        for (VehicleType t : values()) {
            cumulative += t.percentage;
            if (r < cumulative) return t;
        }
        // fallback defensivo
        return CARRO;
    }
}
