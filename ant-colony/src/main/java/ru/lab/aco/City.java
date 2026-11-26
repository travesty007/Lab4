package ru.lab.aco;

/** Неизменяемый узел графа. Координаты одновременно задают веса ребер. */
public record City(int id, double x, double y) {
    public City {
        if (id < 0 || !Double.isFinite(x) || !Double.isFinite(y))
            throw new IllegalArgumentException("Некорректный город");
    }
    public double distanceTo(City other) { return Math.hypot(x - other.x, y - other.y); }
}
