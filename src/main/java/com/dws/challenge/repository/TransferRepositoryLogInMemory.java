package com.dws.challenge.repository;

import com.dws.challenge.domain.TransferLog;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Validated
public class TransferRepositoryLogInMemory implements TransferRepositoryLog {
    private static String KEY_SEP = ":";
    //access for the unit-test
    @Getter
    private final Map<String, List<TransferLog>> accountToTransfers = new HashMap<>();

    @Override
    public TransferLog create(@NonNull String fromAccountId, @NonNull String toAccountId, @NonNull BigDecimal amount) {
        return new TransferLog(fromAccountId, toAccountId, amount);
    }

    @Override
    public List<TransferLog> findFor(String fromAccountId, String toAccountId) {
        return findFor(fromAccountId, toAccountId, LocalDateTime.MIN, LocalDateTime.now());
    }

    private Boolean matchByDate(LocalDateTime accountUpdateDate, LocalDateTime fromDate, @NonNull LocalDateTime toDate) {
        return accountUpdateDate.compareTo(fromDate) >= 0 && accountUpdateDate.compareTo(toDate) <= 0;
    }

    @Override
    final public List<TransferLog> findFor(@NonNull String fromAccountId, @NonNull String toAccountId, @NonNull LocalDateTime fromDate, @NonNull LocalDateTime toDate) {
        synchronized (accountToTransfers) {
            String key = createKey(fromAccountId, toAccountId);
            //linear search as example, it may be faster.
            return accountToTransfers.getOrDefault(key, new ArrayList<>()).stream().filter(rec -> matchByDate(rec.getUpdatedAt(), fromDate, toDate)).collect(Collectors.toList());
        }
    }

    /**
     * Append transactionLog for an transfer by using accountId1 and accountId2 as key.
     * @param transferLog
     */
    @Override
    public void append(@NonNull TransferLog transferLog) {
        synchronized (accountToTransfers) {
            String key = createKey(transferLog.getFromAccountId(), transferLog.getToAccountId());
            List<TransferLog> existingLogs = accountToTransfers.getOrDefault(key, new ArrayList<>());
            existingLogs.add(transferLog);
            accountToTransfers.put(key, existingLogs);
        }
    }

    private String createKey(String fromAccountId, String toAccountId) {
        return fromAccountId + KEY_SEP + toAccountId;
    }
}
