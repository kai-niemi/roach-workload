package io.roach.workload.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CallMetric {
    private static String separator(int len) {
        return new String(new char[len]).replace('\0', '-');
    }

    private static final String HEADER_PATTERN = "%-40s %7s %8s %10s %10s %10s %10s %10s %11s %11s";

    private static final String ROW_PATTERN = "%-40s %7.0f %c%7.1f %10.1f %10.2f %10.2f %10.2f %10.2f %,11d %,11d";

    private static final String FOOTER_PATTERN = "%-40s %7.0f %c%7.1f %10.1f %10.2f %10.2f %10.2f %10.2f %,11d %,11d";

    public static final int FRAME_SIZE = 500_000;

    private final Map<String, Context> metrics = Collections.synchronizedMap(new TreeMap<>());

    public Context add(String name) {
        return metrics.computeIfAbsent(name, supplier -> new Context(name));
    }

    public void remove(String name) {
        metrics.remove(name);
    }

    public void clear() {
        metrics.clear();
    }

    public String prettyPrintHeader() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf(Locale.US,
                HEADER_PATTERN,
                "metric",
                "time(s)",
                "op/s",
                "op/m",
                "p50(ms)",
                "p90(ms)",
                "p99(ms)",
                "mean(ms)",
                "ok",
                "fail"
        );
        pw.println();
        pw.printf(Locale.US,
                HEADER_PATTERN,
                separator(40),// metric
                separator(7), // time
                separator(8), // ops
                separator(10), // opm
                separator(10), // p50
                separator(10), // p90
                separator(10), // p99
                separator(10), // mean
                separator(11), // success
                separator(11) // fail
        );
        return sw.toString();
    }

    public void prettyPrintBody(Consumer<String> sink) {
        metrics.forEach((key, value) -> sink.accept(value.formatStats()));
    }

    public String prettyPrintFooter() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        double timeAvg = metrics.values().stream().mapToDouble(Context::executionTimeSeconds).average()
                .orElse(0);

        double opsPerSecSum = metrics.values().stream().mapToDouble(Context::opsPerSec).sum();
        double opsPerMinSum = metrics.values().stream().mapToDouble(Context::opsPerMin).sum();

        double p50 = metrics.values().stream().mapToDouble(Context::p50).average().orElse(0);
        double p90 = metrics.values().stream().mapToDouble(Context::p90).average().orElse(0);
        double p99 = metrics.values().stream().mapToDouble(Context::p99).average().orElse(0);
        double meanTime = metrics.values().stream().mapToDouble(Context::mean).average().orElse(0);

        int successSum = metrics.values().stream().mapToInt(Context::callsSuccess).sum();
        int failSum = metrics.values().stream().mapToInt(Context::callsFail).sum();

        pw.printf(Locale.US,
                FOOTER_PATTERN,
                "sum/avg",
                timeAvg,
                ' ',
                opsPerSecSum,
                opsPerMinSum,
                p50,
                p90,
                p99,
                meanTime,
                successSum,
                failSum
        );

        pw.flush();

        return sw.toString();
    }

    public static class Context {
        private final String name;

        private final long startTime;

        private final AtomicInteger callCount = new AtomicInteger();

        private final AtomicInteger callSuccessful = new AtomicInteger();

        private final AtomicInteger callFailed = new AtomicInteger();

        private final List<Snapshot> snapshots = Collections.synchronizedList(new LinkedList<>());

        private Context(String name) {
            this.name = name;
            this.startTime = System.nanoTime();
        }

        public long enter() {
            return System.nanoTime();
        }

        public void exit(long beginTime, Throwable t) {
            if (snapshots.size() > FRAME_SIZE) {
                snapshots.remove(0);
            }
            snapshots.add(new Snapshot(beginTime));

            callCount.incrementAndGet();
            if (t != null) {
                callFailed.incrementAndGet();
            } else {
                callSuccessful.incrementAndGet();
            }
        }

        private double executionTimeSeconds() {
            return Duration.ofNanos(System.nanoTime() - startTime).toMillis() / 1000.0;
        }

        private String formatStats() {
            double p50 = 0;
            double p90 = 0;
            double p99 = 0;

            List<Double> latencies = sortedLatencies();

            if (snapshots.size() > 1) {
                p50 = percentile(latencies, .5);
                p90 = percentile(latencies, .9);
                p99 = percentile(latencies, .99);
            }

            final double opsPerSec = opsPerSec();

            return String.format(Locale.US,
                    ROW_PATTERN,
                    name,
                    executionTimeSeconds(),
                    ' ',
                    opsPerSec,
                    opsPerSec * 60,
                    p50,
                    p90,
                    p99,
                    mean(),
                    callSuccessful.get(),
                    callFailed.get()
            );
        }

        private double opsPerSec() {
            final int size = snapshots.size();
            return size / Math.max(1,
                    Duration.ofNanos(
                                    (System.nanoTime() - (snapshots.isEmpty() ? 0 : snapshots.get(0).endTime)))
                            .toMillis() / 1000.0);
        }

        private double opsPerMin() {
            return opsPerSec() * 60;
        }

        private double p50() {
            return percentile(sortedLatencies(), .5);
        }

        private double p90() {
            return percentile(sortedLatencies(), .9);
        }

        private double p99() {
            return percentile(sortedLatencies(), .99);
        }

        private int callsSuccess() {
            return callSuccessful.get();
        }

        private int callsFail() {
            return callFailed.get();
        }

        private double mean() {
            List<Snapshot> copy = new ArrayList<>(snapshots);
            return copy.stream().mapToDouble(Snapshot::durationMillis)
                    .average()
                    .orElse(0);
        }

        private List<Double> sortedLatencies() {
            List<Snapshot> copy = new ArrayList<>(snapshots);
            return copy.stream().map(Snapshot::durationMillis)
                    .sorted()
                    .collect(Collectors.toList());
        }

        private double percentile(List<Double> latencies, double percentile) {
            if (percentile < 0 || percentile > 1) {
                throw new IllegalArgumentException(">=0 N <=1");
            }
            if (latencies.size() > 0) {
                int index = (int) Math.ceil(percentile * latencies.size());
                return latencies.get(index - 1);
            }
            return 0;
        }
    }

    private static class Snapshot implements Comparable<Snapshot> {
        final long endTime = System.nanoTime();

        final long beginTime;

        Snapshot(long beginTime) {
            if (beginTime > endTime) {
                throw new IllegalArgumentException();
            }
            this.beginTime = beginTime;
        }

        public double durationMillis() {
            return (endTime - beginTime) / 1_000_000.0;
        }

        @Override
        public int compareTo(Snapshot o) {
            return Long.compare(o.endTime, endTime);
        }
    }
}
