package com.dws.challenge.repository;

import com.dws.challenge.domain.TransferLog;
import lombok.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Validated
public class TransferRepositoryLogInMemory implements TransferRepositoryLog {
    private static String KEY_SEP = ":";
    private final Map<String, TransferLog> accountToTransfer = new ConcurrentHashMap<>();

    @Override
    public TransferLog create(@NonNull String fromAccountId, @NonNull String toAccountId, @NonNull BigDecimal amount) {
        return new TransferLog(fromAccountId, toAccountId, amount);
    }

    @Override
    public Optional<TransferLog> findFor(String fromAccountId, String toAccountId) {
        String key = createKey(fromAccountId, toAccountId);
        TransferLog log = accountToTransfer.get(key);
        return log == null ? Optional.empty() : Optional.of(log);
    }


    @Override
    public void save(@NonNull TransferLog transferLog) {
        String key = createKey(transferLog.getFromAccountId(), transferLog.getToAccountId());
        accountToTransfer.put(key, transferLog);
    }

    private String createKey(String fromAccountId, String toAccountId) {
        return fromAccountId + KEY_SEP + toAccountId;
    }
}
