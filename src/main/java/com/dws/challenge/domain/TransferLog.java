package com.dws.challenge.domain;

import lombok.Data;
import lombok.NonNull;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Track account operations to rollback that if a transfer fails.
 */
@Data
public class TransferLog {
    @NonNull
    @NotEmpty
    private final String id;
    @NonNull
    @NotEmpty
    private final String fromAccountId;
    @NonNull
    @NotEmpty
    private final String toAccountId;
    @NonNull
    @Min(value = 0, message = "Initial balance must be positive.")
    private volatile BigDecimal amount;
    @NonNull
    private TransferStatus status;
    @NonNull
    private LocalDateTime updatedAt;

    public TransferLog(String fromAccountId, String toAccountId, BigDecimal amount) {
        this(UUID.randomUUID().toString(), fromAccountId, toAccountId, amount, TransferStatus.START, LocalDateTime.now());
    }

    private TransferLog(String id, String fromAccountId, String toAccountId, BigDecimal amount, TransferStatus status, LocalDateTime updatedAt) {
        this.id = id;
        this.amount = amount;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public TransferLog copyOf() {
        return new TransferLog(this.id, this.fromAccountId, this.toAccountId, this.amount, this.status, this.updatedAt);
    }
}
