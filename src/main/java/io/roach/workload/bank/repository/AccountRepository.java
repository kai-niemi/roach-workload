package io.roach.workload.bank.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.AccountSummary;
import io.roach.workload.bank.model.Region;
import io.roach.workload.common.util.Money;

public interface AccountRepository {
    Money getBalance(Account.Id id);

    BigDecimal getTotalBalance();

    int createAccounts(String region, Money balance, int numAccounts, NamingStrategy namingStrategy);

    List<Account> findAccountsByRegion(String region, int offset, int limit);

    List<Account> findAccountsForUpdate(Set<Account.Id> ids);

    void updateBalances(List<Account> accounts);

    AccountSummary accountSummary(Region region);
}
