package io.roach.workload.bank.repository;

import io.roach.workload.bank.model.Transaction;

public interface TransactionRepository {
    Transaction create(Transaction transaction);
}
