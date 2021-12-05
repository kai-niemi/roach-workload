package io.roach.workload.common.cli;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.shell.Availability;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.commands.Quit;
import org.springframework.util.FileCopyUtils;

import com.zaxxer.hikari.HikariDataSource;

import io.roach.workload.common.util.BoundedExecutor;
import io.roach.workload.common.util.RandomData;

import static java.nio.charset.StandardCharsets.UTF_8;

@ShellComponent
@ShellCommandGroup("Admin Commands")
public class Admin implements Quit.Command {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private BoundedExecutor boundedExecutor;

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private Console console;

    public Availability dataSourceCheck() {
        return hikariDataSource.getHikariPoolMXBean() != null
                ? Availability.available()
                : Availability.unavailable("Not a valid datasource");
    }

    public Availability activeWorkersCheck() {
        return boundedExecutor.hasActiveWorkers()
                ? Availability.available()
                : Availability.unavailable("No active workers");
    }

    public Availability noActiveWorkersCheck() {
        return boundedExecutor.hasActiveWorkers()
                ? Availability.unavailable("Active workers")
                : Availability.available();
    }

    @PostConstruct
    public void init() {
        RandomData.randomCurrency();
    }

    @ShellMethod(value = "Exit the shell", key = {"q", "quit", "exit"})
    public void quit() {
        applicationContext.close();
        throw new ExitRequest();
    }

    @ShellMethod(value = "Print application YAML config")
    public void config() {
        Resource resource = applicationContext.getResource("classpath:application.yml");
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            System.out.println(FileCopyUtils.copyToString(reader));
            System.out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ShellMethod(value = "Cancel active workloads", key = {"cancel", "c", "x"})
    public void cancel() {
        boundedExecutor.cancelAndRestart();
    }
}
