package com.dws.challenge.repository;

import com.dws.challenge.domain.TransferLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CRUD for transaction log,it's used by TransferService to rollback if transfer fails.
 */
public interface TransferRepositoryLog {
    TransferLog create(String fromAccountId, String toAccountId, BigDecimal amount);

    List<TransferLog> findFor(String fromAccountId, String toAccountId);

    List<TransferLog> findFor(String fromAccountId, String toAccountId, LocalDateTime fromDate, LocalDateTime toDate);

    void append(TransferLog transferLog);

}
