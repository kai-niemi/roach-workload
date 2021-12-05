package io.roach.workload.common.util;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Timer implements Iterable<Timer.Fork> {
    private final String name;

    private final Map<String, Fork> forks = new TreeMap<>();

    public Timer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Fork fork(String name) {
        Fork fork;
        if (forks.containsKey(name)) {
            fork = forks.get(name);
        } else {
            fork = new Fork(name);
        }
        forks.put(name, fork);
        fork.start();
        return fork;
    }

    public Timer stopAll() {
        if (forks.isEmpty()) {
            throw new IllegalStateException();
        }
        forks.values().forEach(Fork::stop);
        return this;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder(name).append(": ");
        boolean first = true;

        for (Fork fork : forks.values()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(fork.name).append(": ").append(format(fork.durationMillis())).append(" ms");
        }

        return sb.append(", total: ").append(format(totalTime())).append(" ms").toString();
    }

    public double totalTime() {
        return forks.values().stream().mapToDouble(Fork::durationMillis).sum();
    }

    private String format(double v) {
        return String.format(Locale.US, "%.3f", v);
    }

    @Override
    public Iterator<Fork> iterator() {
        return forks.values().iterator();
    }

    public static class Fork {
        private final String name;

        private long startTime;

        private long stopTime;

        private Fork(String name) {
            this.name = name;
        }

        public Fork start() {
            if (stopTime != 0) {
                this.startTime = this.stopTime;
            } else {
                this.startTime = System.nanoTime();
            }
            return this;
        }

        public Fork stop() {
            if (startTime == 0) {
                throw new IllegalStateException();
            }
            this.stopTime = System.nanoTime();
            return this;
        }

        public String getName() {
            return name;
        }

        public double durationMillis() {
            return (stopTime - startTime) / 1E6;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Fork fork = (Fork) o;

            return name.equals(fork.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
