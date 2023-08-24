package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferLog;
import com.dws.challenge.domain.TransferStatus;
import com.dws.challenge.exception.LockServiceException;
import com.dws.challenge.exception.ServiceException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.repository.TransferRepositoryLog;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Validated
public class TransferServiceImp implements TransferService {
    private static int RETRY_TIMES_TO_TAKE_LOCK = 300;
    @Setter
    @Autowired
    private AdvisoryLockService lockService;

    @Autowired
    @Setter
    private TransferRepositoryLog transferRepositoryLog;

    @Autowired
    @Setter
    private NotificationService notificationService;

    @Autowired
    @Setter
    private AccountsRepository accountsRepository;


    private void debitOrCreditAccount(TransferLog transferLog, Account account, BigDecimal amount, TransferStatus status) {
        transferLog.setStatus(status);
        transferRepositoryLog.save(transferLog);
        account.setBalance(amount);
        accountsRepository.save(account);
    }

    @Override
    public void transfer(@NonNull Account from, @NonNull Account to, @NonNull BigDecimal amount) {
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(List.of(from.getAccountId(), to.getAccountId()), RETRY_TIMES_TO_TAKE_LOCK);
        if (locked.isEmpty())
            throw new LockServiceException("Cannot acquired the lock for the accounts " + List.of(from.getAccountId(), to.getAccountId()).stream().collect(Collectors.joining(",")));

        TransferLog transferLog = transferRepositoryLog.create(from.getAccountId(), to.getAccountId(), amount);
        try {
            //check amount in fromAccount
            if (from.getBalance().compareTo(amount) < 0) {
                notificationService.notifyAboutTransfer(from, "Doesn't have enough money for transfer");
                throw new ServiceException(from + " doesn't contain enough money.");
            }
            //save current state of transfer to track its progress to recover it,if it fails.
            //debit from account
            debitOrCreditAccount(transferLog, from, from.getBalance().subtract(amount), TransferStatus.DEBIT_FROM_ACCOUNT);
            //credit to account
            debitOrCreditAccount(transferLog, to, to.getBalance().add(amount), TransferStatus.CREDIT_TO_ACCOUNT);
            //mark transfer as completed
            transferLog.setStatus(TransferStatus.COMPLETED);
            transferRepositoryLog.save(transferLog);
            notificationService.notifyAboutTransfer(from, "Debited the account by " + amount);
            notificationService.notifyAboutTransfer(to, "Credited the account by " + amount);
        } catch (Exception ex) {
            //rollback all changes
            rollback(transferLog, from, to, amount);
            throw ex;
        } finally {
            lockService.release(locked.get());
        }
    }

    private void updateAccount(Account account, BigDecimal amount, LocalDateTime transferLogUpdateAt) {
        if (account.getUpdateAt().compareTo(transferLogUpdateAt) >= 0) {
            try {
                account.setBalance(amount);
                account.setUpdateAt(LocalDateTime.now());
                accountsRepository.save(account);
            } catch (Exception ignored) {
            }
        }
    }

    private void rollback(TransferLog transferLog, Account fromAccount, Account toAccount, BigDecimal amount) {
        LocalDateTime transferLogUpdateAt = transferLog.getUpdatedAt();
        TransferStatus status = transferLog.getStatus();
        //if it debited fromAccount,let's rollback it.
        if (status.equals(TransferStatus.DEBIT_FROM_ACCOUNT) || status.equals(TransferStatus.CREDIT_TO_ACCOUNT)) {
            fromAccount = accountsRepository.getAccount(fromAccount.getAccountId());
            if (fromAccount != null) {
                //if account is updated after transfer log update date, it has been debited.
                updateAccount(fromAccount, fromAccount.getBalance().add(amount), transferLogUpdateAt);
            }
        }
        //if it credited toAccount,let's rollback it.
        if (status.equals(TransferStatus.CREDIT_TO_ACCOUNT)) {
            toAccount = accountsRepository.getAccount(toAccount.getAccountId());
            if (toAccount != null) {
                //if account is updated after transfer log update date, it has been credited.
                updateAccount(toAccount, toAccount.getBalance().subtract(amount), transferLogUpdateAt);
            }
        }
        transferLog.setStatus(TransferStatus.FAILED);
        transferRepositoryLog.save(transferLog);

    }
}
