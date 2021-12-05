package io.roach.workload.bank.service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.Pair;
import io.roach.workload.bank.model.Transaction;
import io.roach.workload.bank.model.TransactionRequest;
import io.roach.workload.bank.repository.AccountRepository;
import io.roach.workload.bank.repository.TransactionRepository;
import io.roach.workload.common.aspect.TransactionBoundary;
import io.roach.workload.common.util.Money;

@Profiles.Bank
@TransactionBoundary
public class TransactionServiceImpl implements TransactionService {
    private AccountRepository accountRepository;

    private TransactionRepository transactionRepository;

    public TransactionServiceImpl(AccountRepository accountRepository,
                                  TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @TransactionBoundary
    public Transaction submitTransaction(TransactionRequest request) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("No transaction context - check Spring profile settings");
        }

        if (request.getAccountLegs().size() < 2) {
            throw new BadRequestException("Must have at least two account items");
        }

        // Coalesce multi-legged transactions
        final Map<Account.Id, Pair<Money, String>> legs = coalesce(request);

        // Lookup accounts with authoritative reads
        final List<Account> accounts = accountRepository.findAccountsForUpdate(legs.keySet());

        final Transaction.Id id = Transaction.Id.of(request.getUuid(), request.getRegion());

        final Transaction.Builder transactionBuilder = Transaction.builder()
                .withId(id)
                .withTransferType(request.getTransactionType())
                .withBookingDate(request.getBookingDate())
                .withTransferDate(request.getTransferDate());

        legs.forEach((accountId, value) -> {
            final Money amount = value.getLeft();

            Account account = accounts.stream().filter(a -> Objects.equals(a.getId(), accountId))
                    .findFirst().orElseThrow(() -> new NoSuchAccountException(accountId.toString()));

            transactionBuilder
                    .andItem()
                    .withAccount(account)
                    .withRunningBalance(account.getBalance())
                    .withAmount(amount)
                    .withNote(value.getRight())
                    .then();

            account.addAmount(amount);
        });

        accountRepository.updateBalances(accounts);

        return transactionRepository.create(transactionBuilder.build());
    }

    private Map<Account.Id, Pair<Money, String>> coalesce(TransactionRequest request) {
        final Map<Account.Id, Pair<Money, String>> legs = new HashMap<>();
        final Map<Currency, BigDecimal> amounts = new HashMap<>();

        // Compact accounts and verify that total balance for the legs with the same currency is zero
        request.getAccountLegs().forEach(leg -> {
            legs.compute(Account.Id.of(leg.getId(), leg.getRegion()),
                    (key, amount) -> (amount == null)
                            ? Pair.of(leg.getAmount(), leg.getNote())
                            : Pair.of(amount.getLeft().plus(leg.getAmount()), leg.getNote()));
            amounts.compute(leg.getAmount().getCurrency(),
                    (currency, amount) -> (amount == null)
                            ? leg.getAmount().getAmount() : leg.getAmount().getAmount().add(amount));
        });

        // The sum of debits for all accounts must equal the corresponding sum of credits (per currency)
        amounts.forEach((key, value) -> {
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Unbalanced transaction: currency ["
                        + key + "], amount sum [" + value + "]");
            }
        });

        return legs;
    }
}
