package ru.lab.aco;

import java.util.*;

/** Модель не зависит от JavaFX. Феромон меняется только после полного цикла. */
public final class AntColony {
    public record Statistics(int iteration, double best, double mean, double matchingPercent) {}
    private final Graph graph;
    private final Parameters parameters;
    private final Random random;
    private List<Ant> ants = List.of();
    private List<Integer> bestRoute = List.of();
    private double bestLength = Double.POSITIVE_INFINITY;
    private final List<Statistics> history = new ArrayList<>();
    private int iteration;
    private boolean pending;
    public AntColony(Graph graph, Parameters parameters) {
        this.graph = graph; this.parameters = parameters; random = new Random(parameters.seed()); graph.reset();
    }
    /** Планирование цикла без обновления феромона для последующей анимации. */
    public void beginIteration() {
        if (pending || finished()) throw new IllegalStateException("Цикл недоступен");
        List<Ant> population = new ArrayList<>();
        for (int k = 0; k < parameters.ants(); k++) {
            Ant ant = new Ant(k % graph.size(), graph.size());
            ant.buildTour(graph, parameters, random, iteration == 0);
            population.add(ant);
        }
        ants = List.copyOf(population); pending = true;
    }
    public void completeIteration() {
        if (!pending) throw new IllegalStateException("Цикл не начат");
        double total = 0;
        for (Ant ant : ants) {
            total += ant.length();
            if (ant.length() < bestLength) { bestLength = ant.length(); bestRoute = ant.route(); }
        }
        graph.edges().forEach(e -> e.evaporate(parameters.evaporation()));
        for (Ant ant : ants) {
            List<Integer> route = ant.route();
            for (int i = 1; i < route.size(); i++)
                graph.edge(route.get(i - 1), route.get(i)).deposit(parameters.q() / ant.length());
        }
        Set<Long> bestEdges = edgeSet(bestRoute);
        long matching = ants.stream().filter(a -> edgeSet(a.route()).equals(bestEdges)).count();
        history.add(new Statistics(++iteration, bestLength, total / ants.size(), 100.0 * matching / ants.size()));
        pending = false;
    }
    private static Set<Long> edgeSet(List<Integer> route) {
        Set<Long> result = new HashSet<>();
        for (int i = 1; i < route.size(); i++) {
            int a = route.get(i-1), b = route.get(i);
            result.add(((long)Math.min(a,b) << 32) | Math.max(a,b));
        }
        return result;
    }
    public void step() { beginIteration(); completeIteration(); }
    public Graph graph() { return graph; }
    public List<Ant> ants() { return ants; }
    public List<Integer> bestRoute() { return bestRoute; }
    public double bestLength() { return bestLength; }
    public int iteration() { return iteration; }
    public boolean pending() { return pending; }
    public boolean finished() { return iteration >= parameters.iterations(); }
    public List<Statistics> history() { return List.copyOf(history); }
}
