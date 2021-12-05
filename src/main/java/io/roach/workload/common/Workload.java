package io.roach.workload.common;

public interface Workload {
    void info();

    Metadata getMetadata();

    interface Metadata {
        String prompt();

        String name();

        String description();
    }
}
