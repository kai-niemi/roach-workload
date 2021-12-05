package io.roach.workload.bank.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Transaction;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
//@Profile(ProfileNames.JPA)
@Profiles.Bank
public class JpaTransactionRepositoryAdapter implements TransactionRepository {
    @Autowired
    private TransactionJpaRepository transactionRepository;

    @Autowired
    private TransactionItemJpaRepository itemRepository;

    @Override
    public Transaction create(Transaction transaction) {
        transaction.getItems().forEach(transactionItem -> itemRepository.save(transactionItem));
        return transactionRepository.save(transaction);
    }
}
