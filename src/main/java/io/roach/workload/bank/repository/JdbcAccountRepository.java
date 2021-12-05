package io.roach.workload.bank.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.bank.model.Account;
import io.roach.workload.bank.model.AccountSummary;
import io.roach.workload.bank.model.AccountType;
import io.roach.workload.bank.model.Region;
import io.roach.workload.common.aspect.TransactionBoundary;
import io.roach.workload.common.util.Money;
import io.roach.workload.common.util.RandomData;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
@Profiles.Bank
public class JdbcAccountRepository implements AccountRepository {
    private JdbcTemplate jdbcTemplate;

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Money getBalance(Account.Id id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT balance,currency "
                        + "FROM account a "
                        + "WHERE id=? AND a.region=?",
                (rs, rowNum) -> Money.of(rs.getString(1), rs.getString(2)),
                id.getUUID(), id.getRegion()
        );
    }

    @Override
    public BigDecimal getTotalBalance() {
        return this.jdbcTemplate.queryForObject(
                "SELECT sum(balance) "
                        + "FROM account a "
                        + "WHERE 1=1",
                (rs, rowNum) -> rs.getBigDecimal(1)
        );
    }

    @Override
    @TransactionBoundary
    public int createAccounts(String region, Money initialBalance, int numAccounts, NamingStrategy namingStrategy) {
        final AtomicInteger batchSize = new AtomicInteger(256);
        if (numAccounts < batchSize.get()) {
            batchSize.set(numAccounts);
        }

        int total = 0;
        for (int i = 0; i < numAccounts; i += batchSize.get()) {
            if (i + batchSize.get() >= numAccounts) {
                batchSize.set(numAccounts - i);
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO account "
                            + "(id, region, balance, currency, name, description, type, closed, allow_negative, updated) "
                            + "VALUES(?,?,?,?,?,?,?,?,?,?)",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setObject(1, UUID.randomUUID());
                            ps.setString(2, region);
                            ps.setBigDecimal(3, initialBalance.getAmount());
                            ps.setString(4, initialBalance.getCurrency().getCurrencyCode());
                            ps.setString(5, namingStrategy.accountName(i));
                            ps.setString(6, RandomData.randomWord(12));
                            ps.setString(7, AccountType.ASSET.getCode());
                            ps.setBoolean(8, false);
                            ps.setInt(9, 0);
                            ps.setTimestamp(10, Timestamp.from(Instant.now()));
                        }

                        @Override
                        public int getBatchSize() {
                            return batchSize.get();
                        }
                    });
            total += batchSize.get();
        }
        return total;
    }

    @Override
    public void updateBalances(List<Account> accounts) {
        int[] rowsAffected = jdbcTemplate.batchUpdate(
                "UPDATE account "
                        + "SET "
                        + "   balance = ?,"
                        + "   updated=? "
                        + "WHERE id = ? "
                        + "   AND region=? "
                        + "   AND closed=false "
                        + "   AND currency=? "
                        + "   AND (?) * abs(allow_negative-1) >= 0", // RETURNING NOTHING
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Account account = accounts.get(i);

                        ps.setBigDecimal(1, account.getBalance().getAmount());
                        ps.setObject(2, LocalDateTime.now());
                        ps.setObject(3, account.getUUID());
                        ps.setString(4, account.getRegion());
                        ps.setString(5, account.getBalance().getCurrency().getCurrencyCode());
                        ps.setBigDecimal(6, account.getBalance().getAmount());
                    }

                    @Override
                    public int getBatchSize() {
                        return accounts.size();
                    }
                });

        // Trust but verify
        Arrays.stream(rowsAffected).filter(i -> i != 1).forEach(i -> {
            throw new IncorrectResultSizeDataAccessException(1, i);
        });
    }

    @Override
    public List<Account> findAccountsForUpdate(Set<Account.Id> ids) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        parameters.addValue("ids",
                ids.stream().map(Account.Id::getUUID).collect(Collectors.toSet()));
        parameters.addValue("regions",
                ids.stream().map(Account.Id::getRegion).collect(Collectors.toSet()));

        return this.namedParameterJdbcTemplate.query(
                "SELECT * FROM account WHERE id in (:ids) AND region in (:regions) FOR UPDATE",
                parameters,
                (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public List<Account> findAccountsByRegion(String region, int offset, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("region", region);
        parameters.addValue("offset", offset);
        parameters.addValue("limit", limit);
        return this.namedParameterJdbcTemplate.query(
                "SELECT * FROM account WHERE region=:region "
                        + "OFFSET (:offset) LIMIT (:limit)",
                parameters, (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public AccountSummary accountSummary(Region region) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("region", region.name());

        return namedParameterJdbcTemplate.queryForObject(
                "SELECT "
                        + "  count(a.id) tot_accounts, "
                        + "  sum(a.balance) tot_balance, "
                        + "  min(a.balance) min_balance, "
                        + "  max(a.balance) max_balance, "
                        + "  avg(a.balance) avg_balance "
                        + "FROM account a "
                        + "WHERE a.region=:region",
                parameters,
                (rs, rowNum) -> {
                    AccountSummary summary = new AccountSummary();
                    summary.setNumberOfAccounts(rs.getInt(1));
                    summary.setTotalBalance(rs.getBigDecimal(2));
                    summary.setMinBalance(rs.getBigDecimal(3));
                    summary.setMaxBalance(rs.getBigDecimal(4));
                    summary.setAvgBalance(rs.getBigDecimal(5));
                    return summary;
                });
    }

    private Account readAccount(ResultSet rs) throws SQLException {
        return Account.builder()
                .withId((UUID) rs.getObject("id"), rs.getString("region"))
                .withName(rs.getString("name"))
                .withBalance(Money.of(rs.getString("balance"), rs.getString("currency")))
                .withAccountType(AccountType.of(rs.getString("type")))
                .withDescription(rs.getString("description"))
                .withClosed(rs.getBoolean("closed"))
                .withAllowNegative(rs.getInt("allow_negative") > 0)
                .withUpdated(rs.getTimestamp("updated").toLocalDateTime())
                .build();
    }
}
