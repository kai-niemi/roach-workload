package io.roach.workload.bank.service;

public class NoSuchAccountException extends BusinessException {
    public NoSuchAccountException(String name) {
        super("No such account: " + name);
    }
}
