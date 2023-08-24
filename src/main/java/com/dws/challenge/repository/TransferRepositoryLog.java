package com.dws.challenge.repository;

import com.dws.challenge.domain.TransferLog;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * CRUD for transaction log,it's used by TransferService to rollback if transfer fails.
 */
public interface TransferRepositoryLog {
    TransferLog create(String fromAccountId, String toAccountId, BigDecimal amount);

    Optional<TransferLog> findFor(String fromAccountId, String toAccountId);

    void save(TransferLog transferLog);
}
