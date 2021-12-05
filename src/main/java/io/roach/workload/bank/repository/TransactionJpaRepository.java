package io.roach.workload.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Transaction;

@Transactional(propagation = Propagation.MANDATORY)
@Profiles.Bank
public interface TransactionJpaRepository extends JpaRepository<Transaction, Transaction.Id>,
        JpaSpecificationExecutor<Transaction> {
}
