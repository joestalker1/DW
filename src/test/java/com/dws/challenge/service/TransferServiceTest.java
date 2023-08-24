package com.dws.challenge.service;


import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.LockServiceException;
import com.dws.challenge.exception.ServiceException;
import com.dws.challenge.repository.AccountsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;

@SpringBootTest
public class TransferServiceTest {
    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private AdvisoryLockService lockService;

    @Test
    public void testTransferMoney(){
        Account fromAccount = new Account("accId1");
        Account toAccount = new Account("accId2");
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        BigDecimal amount = BigDecimal.valueOf(100);
        fromAccount.setBalance(amount);
        accountsRepository.save(fromAccount);
        accountsRepository.save(toAccount);
        transferService.transfer(fromAccount, toAccount, amount);
        fromAccount = accountsRepository.getAccount(fromAccount.getAccountId());
        toAccount = accountsRepository.getAccount(toAccount.getAccountId());
        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(toAccount.getBalance()).isEqualTo(amount);
    }

    @Test
    public void testDontTransferIfBalanceIsNotEnough() {
        final Account fromAccount = new Account("accId1");
        final Account toAccount = new Account("accId2");
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        final BigDecimal amount = BigDecimal.valueOf(100);
        accountsRepository.save(fromAccount);
        accountsRepository.save(toAccount);
        Exception exp = assertThrows(ServiceException.class, () -> transferService.transfer(fromAccount, toAccount, amount));
        assertThat(exp.getMessage()).contains("doesn't contain enough money.");
        //check that accounts are available after failed transfer
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(fromAccount.getAccountId(), toAccount.getAccountId());
        assertThat(locked).isNotEmpty();
        lockService.release(locked.get());
    }

    @Test
    public void testDontTransferAccountsAreAcquired() {
        final Account fromAccount = new Account("accId1");
        final Account toAccount = new Account("accId2");
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        BigDecimal amount = BigDecimal.valueOf(100);
        fromAccount.setBalance(amount);
        accountsRepository.save(fromAccount);
        accountsRepository.save(toAccount);
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(fromAccount.getAccountId(), toAccount.getAccountId());
        assertThat(locked).isNotEmpty();
        Exception exp = assertThrows(LockServiceException.class, () -> transferService.transfer(fromAccount, toAccount, amount));
        assertThat(exp.getMessage()).startsWith("Cannot acquired the lock for the accounts");
    }

}
