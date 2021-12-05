package io.roach.workload.bank.repository;

public interface NamingStrategy {
    String accountName(int sequence);
}
