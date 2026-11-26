package ru.lab.aco;

public record Parameters(int ants, int iterations, double alpha, double beta,
                         double evaporation, double q, long seed) {
    public Parameters {
        if (ants < 1 || ants > 200 || iterations < 1 || iterations > 5000
                || !Double.isFinite(alpha) || alpha < 0 || alpha > 10
                || !Double.isFinite(beta) || beta < 0 || beta > 10
                || !Double.isFinite(evaporation) || evaporation <= 0 || evaporation >= 1
                || !Double.isFinite(q) || q <= 0 || q > 100000)
            throw new IllegalArgumentException("Муравьи: 1–200; циклы: 1–5000; α, β: 0–10; 0 < ρ < 1; 0 < Q ≤ 100000");
    }
    public static Parameters defaults() { return new Parameters(30, 200, 1, 3, .35, 100, 42); }
}
