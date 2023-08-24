package com.dws.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private AdvisoryLockService lockService;

    private static final ExecutorService execServ = Executors.newFixedThreadPool(5);

    @Test
    void addAccount() {
        Account account = createAccount("Id-123");
        this.accountsService.createAccount(account);
        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    void addAccount_failsOnDuplicateId() {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    @Test
    public void testGetAccount() {
        Account account = createAccount("Id-1234");
        accountsService.createAccount(account);
        assertThat(accountsService.getAccount(account.getAccountId()).getAccountId()).isEqualTo("Id-1234");
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private void sleepFor(long pauseInMillis) {
        try {
            Thread.sleep(pauseInMillis);
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void testGetAccountEvenAccountAcquiredAndReleased() {
        Account account = createAccount("Id-12345");
        accountsService.createAccount(account);
        final CountDownLatch latch = new CountDownLatch(1);
        execServ.submit(() -> {
            Optional<AdvisoryLockService.Token> locked = lockService.acquire(List.of(account.getAccountId()), 1);
            System.out.println(">>>> Accounts locked");
            latch.countDown();
            sleepFor(300);
            lockService.release(locked.get());
        });
        await(latch);
        Account readAccount = accountsService.getAccount(account.getAccountId());
        assertThat(readAccount.getAccountId()).isEqualTo(account.getAccountId());
        assertThat(readAccount.getBalance()).isEqualTo(account.getBalance());
    }

    @Test
    public void testTransfer() {
        final Account account1 = createAccount("Id-123456790");
        accountsService.createAccount(account1);
        BigDecimal account1Amount = account1.getBalance();
        final Account account2 = createAccount("Id-1234568990");
        accountsService.createAccount(account2);
        final BigDecimal amount = BigDecimal.valueOf(100);
        BigDecimal account2Amount = account2.getBalance();
        accountsService.transfer(account1.getAccountId(), account2.getAccountId(), amount);
        assertThat(account1.getBalance()).isEqualTo(account1Amount.subtract(amount));
        assertThat(account2.getBalance()).isEqualTo(account2Amount.add(amount));
    }

    @Test
    public void testTransferIfAccountIsNull() {
        Exception exp1 = assertThrows(NullPointerException.class, () -> accountsService.transfer(null, null, BigDecimal.ONE));
        assertThat(exp1.getMessage()).startsWith("fromAccountId is marked non-null but is null");

        final Account account1 = createAccount("Id-19999");
        accountsService.createAccount(account1);
        BigDecimal amount = account1.getBalance().add(BigDecimal.valueOf(100));
        final Account account2 = createAccount("Id-11010106");
        accountsService.createAccount(account2);
        Exception exp2 = assertThrows(ServiceException.class, () -> accountsService.transfer(account1.getAccountId(), account2.getAccountId(), amount));
        assertThat(exp2.getMessage()).contains("doesn't contain enough money");
    }

    @Test
    public void testTransferWhileMultipleGetReadAccountHappens() {
        final Account account1 = createAccount("Id-1234567");
        accountsService.createAccount(account1);
        final Account account2 = createAccount("Id-12345689");
        accountsService.createAccount(account2);
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < 5; i++) {
            execServ.submit(() -> {
                latch.countDown();
                for (int j = 0; j < 10; j++) {
                    System.out.println(">>>> Thread " + Thread.currentThread().getName() + " call getAccount:" + accountsService.getAccount(account1.getAccountId()));
                    sleepFor(100);
                    accountsService.getAccount(account2.getAccountId());
                }
            });
        }
        await(latch);
        BigDecimal amount = BigDecimal.valueOf(10);
        BigDecimal initialAmountOfAccount1 = account1.getBalance();
        for (int i = 0; i < 5; i++) {
            accountsService.transfer(account1.getAccountId(), account2.getAccountId(), BigDecimal.valueOf(10));
            Account accoun1Changed = accountsService.getAccount(account1.getAccountId());
            sleepFor(300);
            assertThat(accoun1Changed.getBalance()).isEqualTo(initialAmountOfAccount1.subtract(amount.multiply(BigDecimal.valueOf(i + 1))));
        }
    }

    private Account createAccount(String accId) {
        Account account = new Account(accId);
        account.setBalance(new BigDecimal(1000));
        return account;

    }
}
