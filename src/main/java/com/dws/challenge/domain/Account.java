package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NonNull;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class Account {

    @NotNull
    @NotEmpty
    private final String accountId;

    @NotNull
    @Min(value = 0, message = "Initial balance must be positive.")
    private volatile BigDecimal balance;

    private LocalDateTime updateAt;  //it needs to rollback failed transfer using by TransferLog

    public Account(String accountId) {
        this.accountId = accountId;
        this.balance = BigDecimal.ZERO;
        this.updateAt = LocalDateTime.now();
    }

    @JsonCreator
    public Account(@JsonProperty("accountId") String accountId,
                   @JsonProperty("balance") BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
        this.updateAt = LocalDateTime.now();
    }
}
