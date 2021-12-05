package io.roach.workload.common;

import org.springframework.shell.standard.ShellComponent;

import io.roach.workload.Profiles;

@Profiles.Undefined
@ShellComponent
public class UndefinedWorkload extends AbstractWorkload {
    @Override
    public String prompt() {
        return "undefined:$ ";
    }
}
