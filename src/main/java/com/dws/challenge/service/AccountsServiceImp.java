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
import java.util.List;
import java.util.Optional;

@Service
@Validated
public class AccountsServiceImp implements AccountsService {
    private static int RETRY_TIMES_TO_TAKE_LOCK = 300;
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
     * get the exclusive access and reach out strong consistency.
     * If account can't locked, it throws exception.
     * Transfer money is fast,so this operation waits for short time.
     * RETRY_TIMES_TO_TAKE_LOCK is allows to eliminate the startvation.
     * @param accountId
     * @return
     */
    public Account getAccount(@NonNull String accountId) {
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(List.of(accountId), RETRY_TIMES_TO_TAKE_LOCK);
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
     * First it takes mutual lock,if if can't take the lock,it throws exception, but it's very rare case,
     * because getAccount is very fast operation.
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
