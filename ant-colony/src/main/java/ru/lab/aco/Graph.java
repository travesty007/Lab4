package ru.lab.aco;

import java.util.*;

/** Полный неориентированный евклидов граф. */
public final class Graph {
    private final List<City> cities;
    private final List<Edge> edges = new ArrayList<>();
    private final Edge[][] matrix;
    public Graph(List<City> cities) {
        if (cities.size() < 3 || cities.size() > 50) throw new IllegalArgumentException("Нужно от 3 до 50 городов");
        this.cities = List.copyOf(cities); matrix = new Edge[size()][size()];
        for (int i = 0; i < size(); i++) {
            if (cities.get(i).id() != i) throw new IllegalArgumentException("ID должны идти от 0 по порядку");
            for (int j = 0; j < i; j++) {
                Edge e = new Edge(cities.get(i), cities.get(j));
                edges.add(e); matrix[i][j] = matrix[j][i] = e;
            }
        }
    }
    public static Graph random(int count, long seed) {
        if (count < 3 || count > 50) throw new IllegalArgumentException("Нужно от 3 до 50 городов");
        Random r = new Random(seed); List<City> cities = new ArrayList<>();
        while (cities.size() < count) {
            City c = new City(cities.size(), 45 + r.nextDouble() * 710, 45 + r.nextDouble() * 440);
            if (cities.stream().allMatch(other -> c.distanceTo(other) >= 24)) cities.add(c);
        }
        return new Graph(cities);
    }
    public int size() { return cities.size(); }
    public List<City> cities() { return cities; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public Edge edge(int a, int b) { return matrix[a][b]; }
    public double distance(int a, int b) { return a == b ? 0 : matrix[a][b].length(); }
    public void reset() { edges.forEach(Edge::reset); }
}
