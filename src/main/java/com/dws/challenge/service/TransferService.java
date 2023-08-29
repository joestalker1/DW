package com.dws.challenge.service;

import com.dws.challenge.domain.Account;

import java.math.BigDecimal;

/**
 * Transfer money by using transferLog to rollback a transfer when it fails.
 * If failure happens while transfering money, this service compares account updateAt with
 * corresponding field of TransferLog and make changes back
 * while account updatedAt >= transferLog updateAt.
 * This service saves transferLog before every account balance change,
 * and then updates account balance and updateAt(Write ahead log).
 */
public interface TransferService {
    void transfer(Account from, Account to, BigDecimal amount);
}
