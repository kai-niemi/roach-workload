package io.roach.workload.bank.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Transaction;
import io.roach.workload.bank.model.TransactionItem;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
//@Profile(ProfileNames.NOT_JPA)
@Profiles.Bank
public class JdbcTransactionRepository implements TransactionRepository {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Transaction create(Transaction transaction) {
        final LocalDate bookingDate = transaction.getBookingDate();
        final LocalDate transferDate = transaction.getTransferDate();

        jdbcTemplate.update("INSERT INTO transaction "
                        + "(id,region,booking_date,transfer_date,transaction_type) "
                        + "VALUES(?, ?, ?, ?, ?)",
                transaction.getUUID(),
                transaction.getRegion(),
                bookingDate != null ? bookingDate : LocalDate.now(),
                transferDate != null ? transferDate : LocalDate.now(),
                transaction.getTransferType()
        );

        final List<TransactionItem> items = transaction.getItems();

        jdbcTemplate.batchUpdate(
                "INSERT INTO transaction_item "
                        + "(transaction_region, transaction_id, account_region, account_id, amount, currency, note, running_balance) "
                        + "VALUES(?,?,?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TransactionItem transactionLeg = items.get(i);
                        ps.setString(1, transactionLeg.getId().getTransactionRegion());
                        ps.setObject(2, transactionLeg.getId().getTransactionId());
                        ps.setString(3, transactionLeg.getId().getAccountRegion());
                        ps.setObject(4, transactionLeg.getId().getAccountId());
                        ps.setBigDecimal(5, transactionLeg.getAmount().getAmount());
                        ps.setString(6, transactionLeg.getAmount().getCurrency().getCurrencyCode());
                        ps.setString(7, transactionLeg.getNote());
                        ps.setBigDecimal(8, transactionLeg.getRunningBalance().getAmount());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });

        return transaction;
    }
}
