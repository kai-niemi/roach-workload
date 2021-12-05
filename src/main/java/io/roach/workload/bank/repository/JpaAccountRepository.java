package io.roach.workload.bank.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.AccountSummary;
import io.roach.workload.bank.model.AccountType;
import io.roach.workload.bank.model.Region;
import io.roach.workload.common.aspect.TransactionBoundary;
import io.roach.workload.common.util.Money;

@Service
@Transactional(propagation = Propagation.MANDATORY)
@Profiles.Bank
public class JpaAccountRepository implements AccountRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Override
    public Money getBalance(Account.Id id) {
        return accountRepository.findBalanceById(id);
    }

    @Override
    @TransactionBoundary
    public int createAccounts(String region, Money balance, int numAccounts, NamingStrategy namingStrategy) {
        int batchSize = 256;
        if (numAccounts < batchSize) {
            batchSize = numAccounts;
        }

        Session session = entityManager.unwrap(Session.class);
        session.setJdbcBatchSize(batchSize);

        int total = 0;
        for (int i = 0; i < numAccounts; i++) {
            accountRepository.save(Account.builder()
                    .withId(UUID.randomUUID(), region)
                    .withName(namingStrategy.accountName(i))
                    .withBalance(balance)
                    .withAccountType(AccountType.ASSET)
                    .build());
            total++;
        }

        accountRepository.flush();

        return total;
    }

    @Override
    public void updateBalances(List<Account> accounts) {
        // No-op, expect batch updates via transparent persistence
    }

    @Override
    public List<Account> findAccountsForUpdate(Set<Account.Id> ids) {
        return accountRepository.findAll(
                ids.stream().map(Account.Id::getUUID).collect(Collectors.toSet()),
                ids.stream().map(Account.Id::getRegion).collect(Collectors.toSet()));
    }

    @Override
    public List<Account> findAccountsByRegion(String region, int offset, int limit) {
        return entityManager.createQuery("SELECT a FROM Account a WHERE a.id.region=?1",
                Account.class)
                .setParameter(1, region)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public BigDecimal getTotalBalance() {
        return entityManager.createQuery("SELECT a.balance FROM Account a", BigDecimal.class).getSingleResult();
    }

    @Override
    public AccountSummary accountSummary(Region region) {
        return new AccountSummary();
    }
}
