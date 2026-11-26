package ru.lab.aco;

import java.util.Arrays;

/** Held–Karp: точный эталон длины, O(n² 2^n). Не вмешивается в поиск муравьев. */
public final class ExactSolver {
    private ExactSolver() {}
    public static double solve(Graph graph) {
        int n = graph.size();
        if (n > 14) throw new IllegalArgumentException("Точный расчет доступен до 14 городов");
        int count = 1 << n;
        double[][] dp = new double[count][n];
        for (double[] row : dp) Arrays.fill(row, Double.POSITIVE_INFINITY);
        dp[1][0] = 0;
        for (int mask = 1; mask < count; mask += 2)
            for (int last = 0; last < n; last++) if (Double.isFinite(dp[mask][last]))
                for (int next = 1; next < n; next++) if ((mask & (1 << next)) == 0) {
                    int updated = mask | (1 << next);
                    dp[updated][next] = Math.min(dp[updated][next], dp[mask][last] + graph.distance(last,next));
                }
        double result = Double.POSITIVE_INFINITY;
        for (int last = 1; last < n; last++) result = Math.min(result, dp[count-1][last] + graph.distance(last,0));
        return result;
    }
}
