package io.roach.workload.common;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import io.roach.workload.Profiles;

@Profiles.Undefined
@ShellComponent
public class UndefinedWorkload extends AbstractWorkload {
    @Override
    public Metadata getMetadata() {
        return new Metadata() {
            @Override
            public String prompt() {
                return "undefined";
            }

            @Override
            public String name() {
                return "Undefined";
            }

            @Override
            public String description() {
                return "Please define which workload to use (run with --help)";
            }
        };
    }

    @Override
    @ShellMethod(value = "Print workload info")
    public void info() {
        printInfo();
    }
}
