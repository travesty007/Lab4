package ru.lab.aco;

import java.util.*;

public final class Ant {
    private final int start;
    private final List<Integer> route = new ArrayList<>();
    private final boolean[] visited;
    private double length;
    public Ant(int start, int cityCount) {
        this.start = start; visited = new boolean[cityCount];
        route.add(start); visited[start] = true;
    }
    /** Рулетка по вероятностям; логарифмы предотвращают переполнение весов. */
    public int chooseNext(Graph graph, Parameters p, Random random, boolean uniform) {
        int current = route.getLast();
        double[] weights = new double[graph.size()];
        double max = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < weights.length; j++) {
            weights[j] = Double.NEGATIVE_INFINITY;
            if (!visited[j]) {
                Edge e = graph.edge(current, j);
                weights[j] = uniform ? 0 : p.alpha() * Math.log(e.pheromone()) - p.beta() * Math.log(e.length());
                max = Math.max(max, weights[j]);
            }
        }
        if (!Double.isFinite(max)) throw new IllegalStateException("Все города уже посещены");
        double sum = 0;
        for (int j = 0; j < weights.length; j++) {
            weights[j] = visited[j] ? 0 : Math.exp(weights[j] - max); sum += weights[j];
        }
        double threshold = random.nextDouble() * sum;
        int fallback = -1;
        for (int j = 0; j < weights.length; j++) if (!visited[j]) {
            fallback = j; threshold -= weights[j];
            if (threshold < 0) return j;
        }
        return fallback;
    }
    public void moveTo(int next, Graph graph) {
        if (visited[next]) throw new IllegalArgumentException("Город уже посещен");
        length += graph.distance(route.getLast(), next); route.add(next); visited[next] = true;
    }
    public void buildTour(Graph graph, Parameters p, Random random, boolean uniform) {
        while (route.size() < graph.size()) moveTo(chooseNext(graph, p, random, uniform), graph);
        length += graph.distance(route.getLast(), start); route.add(start);
    }
    public List<Integer> route() { return List.copyOf(route); }
    public double length() { return length; }
}
