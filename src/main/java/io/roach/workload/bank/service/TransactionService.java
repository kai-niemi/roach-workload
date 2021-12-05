package io.roach.workload.bank.service;

import io.roach.workload.bank.model.Transaction;
import io.roach.workload.bank.model.TransactionRequest;

public interface TransactionService {
    Transaction submitTransaction(TransactionRequest transactionRequest);
}
