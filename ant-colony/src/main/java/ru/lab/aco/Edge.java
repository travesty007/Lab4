package ru.lab.aco;

/** Одно общее ребро для обоих направлений движения. */
public final class Edge {
    private final City from, to;
    private final double length;
    private double pheromone = 1.0;
    public Edge(City from, City to) {
        this.from = from; this.to = to; length = from.distanceTo(to);
        if (!(length > 0) || !Double.isFinite(length))
            throw new IllegalArgumentException("Города должны иметь разные координаты");
    }
    public City from() { return from; }
    public City to() { return to; }
    public double length() { return length; }
    public double pheromone() { return pheromone; }
    public void evaporate(double rho) { pheromone = Math.max(1e-12, pheromone * (1 - rho)); }
    public void deposit(double amount) { pheromone += amount; }
    public void reset() { pheromone = 1.0; }
}
