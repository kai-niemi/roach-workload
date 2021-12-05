package io.roach.workload.bank.service;

import java.math.BigDecimal;
import java.util.List;

import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.AccountSummary;
import io.roach.workload.bank.model.Region;
import io.roach.workload.bank.repository.NamingStrategy;
import io.roach.workload.common.util.Money;

public interface AccountService {
    int createAccounts(String region, Money initialBalance, int numAccounts, NamingStrategy namingStrategy);

    List<Account> findAccountsByRegion(String region, int offset, int limit);

    Money getBalance(Account.Id id);

    Money getBalanceSnapshot(Account.Id id);

    BigDecimal getTotalBalance();

    AccountSummary accountSummary(Region region);
}
