package io.roach.workload;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.util.StringUtils;

import io.roach.workload.common.Workload;

@Configuration
@EnableAutoConfiguration(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties
@ComponentScan(basePackages = "io.roach")
public class Application implements PromptProvider {
    private static void printHelpAndExit(String message) {
        System.out.println("Usage: roach-workload.jar <options> [profile <args> && ..]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("--help          this help");
        System.out.println();
        System.out.println("Profile:");
        System.out.println("bank            bank workload - a financial ledger simulation");
        System.out.println("orders          orders workload - multi-table purchase order creation");
        System.out.println("events          events - transactional outbox events");
        System.out.println("query           query - adhoc query execution from file or CLI");
        System.out.println();
        System.out.println("All other options and args are passed to the interactive CLI.");
        System.out.println();
        System.out.println(message);
        System.exit(0);
    }

    public static void main(String[] args) {
        List<String> profiles = new ArrayList<>();
        List<String> argsFinal = new ArrayList<>();

        Arrays.asList(args).forEach(s -> {
            if (s.equals("--help")) {
                printHelpAndExit("");
            }
            if (Profiles.all().contains(s)) {
                profiles.add(s);
            } else {
                argsFinal.add(s);
            }
        });

        if (profiles.isEmpty()) {
            if (!StringUtils.hasLength(System.getProperty("spring.profiles.active"))) {
                printHelpAndExit("Missing profile(s)");
            }
        } else {
            System.setProperty("spring.profiles.active", StringUtils.collectionToCommaDelimitedString(profiles));
        }

        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .headless(true)
                .profiles(profiles.toArray(new String[] {}))
                .logStartupInfo(true)
                .run(argsFinal.toArray(new String[] {}));
    }

    @Autowired
    private Shell shell;

    @Autowired
    @Lazy
    private History history;

    @Autowired
    @Lazy
    private Workload workload;

    @Bean
    @Lazy
    public History history(LineReader lineReader, @Value("${roach.history.file}") String historyPath) {
        lineReader.setVariable(LineReader.HISTORY_FILE, Paths.get(historyPath));
        return new DefaultHistory(lineReader);
    }

    @EventListener
    public void onContextClosedEvent(ContextClosedEvent event) throws IOException {
        history.save();
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return new ProvidedCommandLineRunner(shell);
    }

    @Override
    public AttributedString getPrompt() {
        return new AttributedString(workload.prompt(),
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold());
    }
}

@Order(InteractiveShellApplicationRunner.PRECEDENCE - 2)
class ProvidedCommandLineRunner implements CommandLineRunner {
    private final Shell shell;

    public ProvidedCommandLineRunner(Shell shell) {
        this.shell = shell;
    }

    @Override
    public void run(String... args) throws Exception {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        List<List<String>> commandBlocks = new ArrayList<>();
        List<String> params = new ArrayList<>();

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.startsWith("&&")) {
                commandBlocks.add(new ArrayList<>(params));
                params.clear();
            } else {
                params.add(arg);
            }
        }

        if (!params.isEmpty()) {
            commandBlocks.add(new ArrayList<>(params));
        }

        for (List<String> commandBlock : commandBlocks) {
            shell.run(new StringInputProvider(commandBlock));
        }
    }
}

class StringInputProvider implements InputProvider {
    private final List<String> words;

    private boolean done;

    public StringInputProvider(List<String> words) {
        this.words = words;
    }

    @Override
    public Input readInput() {
        if (!done) {
            done = true;
            return new Input() {
                @Override
                public List<String> words() {
                    return words;
                }

                @Override
                public String rawText() {
                    return StringUtils.collectionToDelimitedString(words, " ");
                }
            };
        } else {
            return null;
        }
    }
}
