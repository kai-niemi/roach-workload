package io.roach.workload.common.util;

import java.util.List;

public abstract class MathUtils {
    private MathUtils() {
    }

    public static double standardDeviation(List<Double> items) {
        if (items.size() == 0) {
            return Double.NaN;
        }
        double avg = items.stream().mapToDouble(Number::doubleValue).average().orElse(0);
        double rawSum = items.stream().mapToDouble((x) -> Math.pow(x.doubleValue() - avg, 2.0)).sum();
        return Math.sqrt(rawSum / (items.size() - 1));
    }
}
