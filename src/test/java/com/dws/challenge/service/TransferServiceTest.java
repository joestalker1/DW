package com.dws.challenge.service;


import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.ServiceException;
import com.dws.challenge.repository.AccountsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class TransferServiceTest {
    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private AdvisoryLockService lockService;

    private static ExecutorService execServ = Executors.newFixedThreadPool(1);

    @Test
    public void testTransferMoney() {
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
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(List.of(fromAccount.getAccountId(), toAccount.getAccountId()), 1);
        assertThat(locked).isNotEmpty();
        lockService.release(locked.get());
    }

    @Test
    public void tesTransferAccountsEvenAccountIsAcquiredAndReleased() {
        final Account fromAccount = new Account("accId1");
        final Account toAccount = new Account("accId2");
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        BigDecimal amount = BigDecimal.valueOf(100);
        fromAccount.setBalance(amount);
        accountsRepository.save(fromAccount);
        accountsRepository.save(toAccount);
        CountDownLatch latch = new CountDownLatch(1);
        execServ.submit(() -> {
            Optional<AdvisoryLockService.Token> locked = lockService.acquire(List.of(fromAccount.getAccountId(), toAccount.getAccountId()), 1);
            System.out.println(">>>>>>Accounts locked");
            latch.countDown();
            sleepFor(200);
            lockService.release(locked.get());
        });
        await(latch);
        transferService.transfer(fromAccount, toAccount, amount);
        Account fromAccount1Changed = accountsRepository.getAccount(fromAccount.getAccountId());
        assertThat(fromAccount1Changed.getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private void sleepFor(long pauiseInMillis) {
        try {
            Thread.sleep(pauiseInMillis);
        } catch (InterruptedException ignored) {
        }

    }
}
