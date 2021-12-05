package io.roach.workload.bank.model;

import java.math.BigDecimal;

public class AccountSummary {
    private int numberOfAccounts;

    private BigDecimal totalBalance;

    private BigDecimal minBalance;

    private BigDecimal maxBalance;

    private BigDecimal avgBalance;

    public int getNumberOfAccounts() {
        return numberOfAccounts;
    }

    public AccountSummary setNumberOfAccounts(int numberOfAccounts) {
        this.numberOfAccounts = numberOfAccounts;
        return this;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public AccountSummary setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
        return this;
    }

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public AccountSummary setMinBalance(BigDecimal minBalance) {
        this.minBalance = minBalance;
        return this;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public AccountSummary setMaxBalance(BigDecimal maxBalance) {
        this.maxBalance = maxBalance;
        return this;
    }

    public BigDecimal getAvgBalance() {
        return avgBalance;
    }

    public AccountSummary setAvgBalance(BigDecimal avgBalance) {
        this.avgBalance = avgBalance;
        return this;
    }
}

