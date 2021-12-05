package io.roach.workload.common;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import io.roach.workload.common.cli.Console;
import io.roach.workload.common.util.BoundedExecutor;

public abstract class AbstractWorkload implements Workload {
    @Autowired
    protected Console console;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected BoundedExecutor boundedExecutor;

    protected final void printInfo() {
        Metadata metadata = getMetadata();
        console.magenta("Workload Info\n");
        console.green("Name: %s\n", metadata.name());
        console.green("Description: %s\n", metadata.description());
    }
}
