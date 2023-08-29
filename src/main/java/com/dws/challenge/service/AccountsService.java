package com.dws.challenge.service;

import com.dws.challenge.domain.Account;

import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Manage account: create it,return account and transfer money from one account to another.
 */
public interface AccountsService {
    void createAccount(Account account);

    Optional<Account> getAccount(String accountId);

    void transfer(String fromAccountId, String toAccountId, @Min(1) BigDecimal amount);
}
