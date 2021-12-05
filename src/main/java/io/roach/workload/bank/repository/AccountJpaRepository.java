package io.roach.workload.bank.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Account;
import io.roach.workload.common.util.Money;

@Transactional(propagation = Propagation.MANDATORY)
@Profiles.Bank
public interface AccountJpaRepository extends JpaRepository<Account, Account.Id>,
        JpaSpecificationExecutor<Account> {

    @Query(value = "select a.balance "
            + "from Account a "
            + "where a.id = ?1")
    Money findBalanceById(Account.Id id);

    @Query(value = "select a "
            + "from Account a "
            + "where a.id.uuid in (?1) and a.id.region in (?2)")
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<Account> findAll(Set<UUID> ids, Set<String> regions);
}
