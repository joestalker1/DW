package com.dws.challenge.service;

import com.dws.challenge.domain.Account;

import java.math.BigDecimal;

/**
 * Manage account: create it,return account and transfer money from one account to other.
 */
public interface AccountsService {
    void createAccount(Account account);

    Account getAccount(String accountId);

    void transfer(String fromAccountId, String toAcccountId, BigDecimal amount);
}
