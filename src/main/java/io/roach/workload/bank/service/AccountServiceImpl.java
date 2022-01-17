package io.roach.workload.bank.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.AccountSummary;
import io.roach.workload.bank.model.Region;
import io.roach.workload.bank.repository.AccountRepository;
import io.roach.workload.bank.repository.NamingStrategy;
import io.roach.workload.common.aspect.TransactionBoundary;
import io.roach.workload.common.util.Money;

@Profiles.Bank
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    public AccountServiceImpl(@Autowired AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public int createAccounts(String region, Money initialBalance, int numAccounts,
                              NamingStrategy namingStrategy) {
        int batchSize = 1024;
        if (numAccounts < batchSize) {
            batchSize = numAccounts;
        }

        int total = 0;
        for (int i = 0; i < numAccounts; i += batchSize) {
            if (i + batchSize >= numAccounts) {
                batchSize = numAccounts - i;
            }
            total += accountRepository.createAccounts(region, initialBalance, batchSize, namingStrategy);
        }

        return total;
    }

    @Override
    @TransactionBoundary(readOnly = true)
    @Transactional
    public List<Account> findAccountsByRegion(String region, int offset, int limit) {
        return accountRepository.findAccountsByRegion(region, offset, limit);
    }

    @Override
    @TransactionBoundary(readOnly = true)
    public Money getBalance(Account.Id id) {
        return accountRepository.getBalance(id);
    }

    @Override
    @TransactionBoundary(followerRead = true)
    public Money getBalanceSnapshot(Account.Id id) {
        return accountRepository.getBalance(id);
    }

    @Override
    @TransactionBoundary(readOnly = true)
    public BigDecimal getTotalBalance() {
        return accountRepository.getTotalBalance();
    }

    @Override
    @TransactionBoundary(readOnly = true)
    public AccountSummary accountSummary(Region region) {
        return accountRepository.accountSummary(region);
    }
}
