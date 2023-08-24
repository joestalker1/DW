package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.LockServiceException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Validated
public class AccountsServiceImp implements AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;
    @Getter
    private final TransferService transferService;
    private final AdvisoryLockService lockService;

    @Autowired
    public AccountsServiceImp(AccountsRepository accountsRepository, TransferService transferService, AdvisoryLockService lockService) {
        this.accountsRepository = accountsRepository;
        this.transferService = transferService;
        this.lockService = lockService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    /**
     * Return an account by accountId.First acquire the lock to
     * get the mutual lock and reach out strong consistency.
     * There is some trade-off between consistency and speed:
     * when client app can't get this lock, it may show previous balance
     * for short time interval and retry get balance again.
     * In such way it works as snapshot consistency for account read operations
     * for very short time,because write account operations are completed very fast.
     *
     * @param accountId
     * @return
     */
    public Account getAccount(@NonNull String accountId) {
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(accountId);
        if (locked.isEmpty())
            throw new LockServiceException("Cannot acquired the lock for the account " + accountId);
        try {
            return this.accountsRepository.getAccount(accountId);
        } finally {
            if (locked.isPresent()) lockService.release(locked.get());
        }

    }

    /**
     * Transfer money between accounts,if fromAccount has enough balance.
     * First it takes mutual lock, if it can't throw the exception.
     *
     * @param fromAccountId
     * @param toAccountId
     * @param amount
     */
    public void transfer(@NonNull String fromAccountId, @NonNull String toAccountId, @NonNull BigDecimal amount) {
        Account fromAccount = accountsRepository.getAccount(fromAccountId);
        Account toAccount = accountsRepository.getAccount(toAccountId);
        transferService.transfer(fromAccount, toAccount, amount);
    }
}
