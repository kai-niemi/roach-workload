package io.roach.workload.bank;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.AccountSummary;
import io.roach.workload.bank.model.Region;
import io.roach.workload.bank.model.TransactionRequest;
import io.roach.workload.bank.service.AccountService;
import io.roach.workload.bank.service.BadRequestException;
import io.roach.workload.bank.service.TransactionService;
import io.roach.workload.common.AbstractWorkload;
import io.roach.workload.common.jpa.DataSourceHelper;
import io.roach.workload.common.util.DurationFormat;
import io.roach.workload.common.util.Money;
import io.roach.workload.common.util.Multiplier;
import io.roach.workload.common.util.RandomData;

@ShellComponent
@ShellCommandGroup("Workload")
@Profiles.Bank
public class BankWorkload extends AbstractWorkload {
    private static final List<String> QUOTES = Arrays.asList(
            "Cockroaches can eat anything",
            "Roaches can live up to a week without their head",
            "There are more than 4,000 species of cockroaches worldwide",
            "Cockroaches can run up to three miles in an hour"
    );

    @Autowired
    @Qualifier("transactionServiceJdbc")
    private TransactionService transactionServiceJdbc;

    @Autowired
    @Qualifier("transactionServiceJpa")
    private TransactionService transactionServiceJpa;

    @Autowired
    @Qualifier("accountServiceJdbc")
    private AccountService accountServiceJdbc;

    @Autowired
    @Qualifier("accountServiceJpa")
    private AccountService accountServiceJpa;

    @Override
    public Metadata getMetadata() {
        return new Metadata() {
            @Override
            public String prompt() {
                return "bank";
            }

            @Override
            public String name() {
                return "Bank";
            }

            @Override
            public String description() {
                return "Creates multi-legged monetary transactions between random accounts while conserving the total amount";
            }
        };
    }

    @Override
    @ShellMethod(value = "Print workload info")
    public void info() {
        printInfo();
    }

    @ShellMethod(value = "Initialize bank workload")
    public void init(
            @ShellOption(help = "number of accounts per region", defaultValue = "10_000") String accounts,
            @ShellOption(help = "regions to use (all|main|<any>)", defaultValue = "all") String regions,
            @ShellOption(help = "initial account balance in region currency", defaultValue = "100000.00") String initialBalance,
            @ShellOption(help = "data access method (jdbc|jpa)", defaultValue = "jdbc") String method,
            @ShellOption(help = "drop schema", defaultValue = "false") boolean drop,
            @ShellOption(help = "skip create schema", defaultValue = "false") boolean skipCreate
    ) {

        if (drop) {
            console.green("Dropping tables..\n");
            DataSourceHelper.executeScripts(dataSource, "db/bank/drop-bank.sql");
        }
        if (!skipCreate) {
            console.green("Creating tables..\n");
            DataSourceHelper.executeScripts(dataSource, "db/bank/create-bank.sql");
        }

        final AccountService accountService = getAccountService(method);

        final List<Region> matchingRegions = matchingRegions(regions);

        int accountsPerRegion = Multiplier.parseInt(accounts);

        console.green("Creating %,d accounts per %d regions: %s\n",
                accountsPerRegion, matchingRegions.size(), matchingRegions);

        AtomicInteger total = new AtomicInteger();

        matchingRegions.forEach(region -> {
            console.green("Creating %,d accounts in %s with currency %s\n",
                    accountsPerRegion, region, region.currency());

            Money balance = Money.of(initialBalance, region.currency());
            int tot = accountService.createAccounts(
                    region.name(), balance, accountsPerRegion,
                    sequence -> "user:" + sequence);
            total.addAndGet(tot);
        });

        console.green("Bank schema ready - %,d account(s) in total\n", total.get());
    }

    private AccountService getAccountService(String api) {
        return "jdbc".equals(api) ? this.accountServiceJdbc : this.accountServiceJpa;
    }

    private TransactionService getTransactionService(String api) {
        return "jdbc".equals(api) ? this.transactionServiceJdbc : this.transactionServiceJpa;
    }

    private List<Region> matchingRegions(String regions) {
        final List<Region> matchingRegions = new ArrayList<>();
        if ("all".equals(regions)) {
            matchingRegions.addAll(Arrays.asList(Region.values()));
        } else {
            Arrays.stream(regions.split(",")).forEach(r -> matchingRegions.add(Region.valueOf(r)));
        }
        return matchingRegions;
    }

    @ShellMethod(value = "Run bank workload")
    public void run(
            @ShellOption(help = "number of threads per region", defaultValue = "1") int threads,
            @ShellOption(help = "data access method (jdbc|jpa)", defaultValue = "jdbc") String method,
            @ShellOption(help = "main region to use (random|<any>)", defaultValue = "random") String mainRegion,
            @ShellOption(help = "regions to use (all|main|<any>)", defaultValue = "all") String regions,
            @ShellOption(help = "number of account legs per region (multiple of 2)", defaultValue = "2") int legs,
            @ShellOption(help = "execution duration", defaultValue = "30m") String duration,
            @ShellOption(help = "enable verbose logging", defaultValue = "false") boolean trace
    ) {

        final List<Region> matchingRegions = matchingRegions(regions);

        final Region transactionRegion = "random".equals(mainRegion)
                ? RandomData.selectRandom(matchingRegions)
                : Region.valueOf(mainRegion);

        if (legs % 2 != 0) {
            throw new BadRequestException("Accounts per region must be a multiple of 2: " + legs);
        }
        if (matchingRegions.isEmpty()) {
            throw new BadRequestException("No matching regions: " + regions);
        }

        final TransactionService transactionService = getTransactionService(method);

        final AccountService accountService = getAccountService(method);

        final CountDownLatch latch = new CountDownLatch(matchingRegions.size() * threads);

        console.green("Scheduling workers for regions: %s\n", matchingRegions);

        matchingRegions
                .parallelStream()
                .forEach(region -> {
                    final List<Account> regionAccounts = Collections
                            .unmodifiableList(accountService.findAccountsByRegion(region.name(), 0, 5000));

                    Duration runtimeDuration = DurationFormat.parseDuration(duration);

                    final Runnable unitOfWork = () -> {
                        TransactionRequest.Builder requestBuilder = TransactionRequest.builder()
                                .withRegion(transactionRegion.name())
                                .withTransactionType("ABC")
                                .withBookingDate(LocalDate.now())
                                .withTransferDate(LocalDate.now());

                        final List<Account> temporalAccounts = new ArrayList<>(regionAccounts);
                        final Currency currency = temporalAccounts.get(0).getBalance().getCurrency();
                        final Money transferAmount = RandomData.randomMoneyBetween("1.00", "10.00", currency);

                        IntStream.range(0, legs).forEach(value -> {
                            // Debits gravitate towards accounts with highest balance
                            Account account = value % 2 == 0
                                    ? RandomData.selectRandomWeighted(temporalAccounts)
                                    : RandomData.selectRandom(temporalAccounts);
                            temporalAccounts.remove(account);

                            Money amount = value % 2 == 0 ? transferAmount.negate() : transferAmount;

                            requestBuilder
                                    .addLeg()
                                    .withId(account.getUUID(), account.getRegion())
                                    .withAmount(amount)
                                    .withNote(RandomData.selectRandom(QUOTES))
                                    .then();

                            if (trace) {
                                console.green("%s %s to %s, balance before: %s, after: %s\n",
                                        amount.isNegative() ? "Debit" : "Credit",
                                        amount,
                                        account.getName(),
                                        account.getBalance(),
                                        account.getBalance().plus(amount));
                            }
                        });

                        transactionService.submitTransaction(requestBuilder.build());
                    };

                    IntStream.rangeClosed(1, threads).forEach(value -> {
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        boundedExecutor.submit(unitOfWork, "writer (" + region.name() + ") " + value, runtimeDuration);
                    });
                });

        console.green("Let it rip!\n");
        latch.countDown();
    }

    @ShellMethod(value = "Run balance query workload")
    public void balance(
            @ShellOption(help = "number of threads per region", defaultValue = "1") int threads,
            @ShellOption(help = "data access method (jdbc|jpa)", defaultValue = "jdbc") String method,
            @ShellOption(help = "regions to use (all|main|<any>)", defaultValue = "all") String regions,
            @ShellOption(help = "execution duration", defaultValue = "30m") String duration,
            @ShellOption(help = "use follower reads", defaultValue = "false") boolean followerReads
    ) {
        final List<Region> matchingRegions = matchingRegions(regions);
        if (matchingRegions.isEmpty()) {
            throw new BadRequestException("No matching regions: " + regions);
        }

        final AccountService accountService = getAccountService(method);

        final Duration runtimeDuration = DurationFormat.parseDuration(duration);

        final LinkedBlockingQueue<Money> accountBalances = new LinkedBlockingQueue<>();

        matchingRegions
                .parallelStream()
                .forEach(region -> {
                    final List<Account> regionAccounts = Collections
                            .unmodifiableList(accountService.findAccountsByRegion(region.name(), 0, 5000));

                    final Runnable unitOfWork = () -> {
                        Account account = RandomData.selectRandom(regionAccounts);
                        Money balance;
                        if (followerReads) {
                            balance = accountService.getBalanceSnapshot(account.getId());
                        } else {
                            balance = accountService.getBalance(account.getId());
                        }
                        accountBalances.offer(balance);
                    };

                    IntStream.rangeClosed(1,threads).forEach(value -> {
                        boundedExecutor.submit(unitOfWork, followerReads
                                ? "snapshot " : "" + "reader (" + region.name() + ") " + value, runtimeDuration);
                    });
                });

        boundedExecutor.submit(() -> {
            try {
                Money balance = accountBalances.take();
                if (balance.isNegative()) {
                    console.red("OMG!! negative balance detected: %s\n", balance);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "balance checker", runtimeDuration);
    }

    @ShellMethod(value = "Print total bank balance sheet")
    public void summary() {
        AtomicInteger totalAccounts = new AtomicInteger();

        console.green("Total balance per region\n");

        Arrays.stream(Region.values()).sequential().forEach(region -> {
            AccountSummary accountSummary = accountServiceJdbc.accountSummary(region);
            console.green("region: %s currency: %s\n", region, region.currency());
            console.green("\tnumberOfAccounts: %s\n", accountSummary.getNumberOfAccounts());
            console.green("\ttotalBalance: %s\n", accountSummary.getTotalBalance());
            console.green("\tminBalance: %s\n", accountSummary.getMinBalance());
            console.green("\tmaxBalance: %s\n", accountSummary.getMaxBalance());
            console.green("\tavgBalance: %s\n", accountSummary.getAvgBalance());

            totalAccounts.addAndGet(accountSummary.getNumberOfAccounts());
        });

        console.green("Total number of accounts (constant): %,d\n", totalAccounts.get());
        console.green("Total balance of all currencies (constant): %.2f\n", accountServiceJdbc.getTotalBalance());
    }
}

