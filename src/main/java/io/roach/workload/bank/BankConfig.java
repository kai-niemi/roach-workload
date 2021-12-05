package io.roach.workload.bank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.roach.workload.Profiles;
import io.roach.workload.bank.repository.AccountRepository;
import io.roach.workload.bank.repository.TransactionRepository;
import io.roach.workload.bank.service.AccountService;
import io.roach.workload.bank.service.AccountServiceImpl;
import io.roach.workload.bank.service.TransactionService;
import io.roach.workload.bank.service.TransactionServiceImpl;
import io.roach.workload.common.config.AopConfig;
import io.roach.workload.common.config.JpaConfig;

@Configuration
@Import({JpaConfig.class, AopConfig.class})
@EnableJpaRepositories(basePackages = {"io.roach.workload.bank"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Profiles.Bank
public class BankConfig {
    @Autowired
    @Qualifier("jdbcAccountRepository")
    private AccountRepository accountJdbcRepository;

    @Autowired
    @Qualifier("jpaAccountRepository")
    private AccountRepository accountJpaRepository;

    @Autowired
    @Qualifier("jdbcTransactionRepository")
    private TransactionRepository transactionJdbcRepository;

    @Autowired
    @Qualifier("jpaTransactionRepositoryAdapter")
    private TransactionRepository transactionJpaRepository;

    @Bean
    @Primary
    public AccountService accountServiceJdbc() {
        return new AccountServiceImpl(accountJdbcRepository);
    }

    @Bean
    public AccountService accountServiceJpa() {
        return new AccountServiceImpl(accountJpaRepository);
    }

    @Bean
    @Primary
    public TransactionService transactionServiceJdbc() {
        return new TransactionServiceImpl(accountJdbcRepository, transactionJdbcRepository);
    }

    @Bean
    public TransactionService transactionServiceJpa() {
        return new TransactionServiceImpl(accountJpaRepository, transactionJpaRepository);
    }
}
