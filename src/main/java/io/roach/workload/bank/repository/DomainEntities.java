package io.roach.workload.bank.repository;

import javax.transaction.Transaction;

import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.TransactionItem;

public abstract class DomainEntities {
    public static Class[] ENTITY_TYPES = {
            Account.class,
            Transaction.class,
            TransactionItem.class
    };

    private DomainEntities() {
    }
}
