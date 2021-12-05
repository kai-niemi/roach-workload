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
}
