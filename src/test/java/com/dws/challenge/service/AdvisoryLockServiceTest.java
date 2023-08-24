package com.dws.challenge.service;

import com.dws.challenge.exception.LockServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class AdvisoryLockServiceTest {
    @Autowired
    private AdvisoryLockService lockService;

    @Test
    public void testAcquireAndReleaseAccounts() {
        Optional<AdvisoryLockService.Token> locked = lockService.acquire(List.of("accId1", "accId2"), 1);
        assertThat(locked).isNotEmpty();
        lockService.release(locked.get());
    }

    @Test
    public void testCannotAcquireTwoAccountIfOneIsAccquired() {
        assertThat(lockService.acquire(Collections.emptyList(), 1)).isEmpty();
        String accId1 = "accId1";
        Optional<AdvisoryLockService.Token> locked1 = lockService.acquire(List.of(accId1), 1);
        assertThat(locked1).isNotEmpty();
        String accId2 = "accId2";
        Optional<AdvisoryLockService.Token> locked2 = lockService.acquire(List.of(accId1, accId2), 1);
        assertThat(locked2).isEmpty();
        lockService.release(locked1.get());
    }

    @Test
    public void testLockBAIsTheLockABInTheSameOrder() {
        String accId1 = "A";
        String accId2 = "B";
        Optional<AdvisoryLockService.Token> locked1 = lockService.acquire(List.of(accId1, accId2), 1);
        assertThat(locked1).isNotEmpty();
        lockService.release(locked1.get());
        Optional<AdvisoryLockService.Token> locked2 = lockService.acquire(List.of(accId2, accId1), 1);
        assertThat(locked2).isNotEmpty();
        lockService.release(locked2.get());
        assertThat(locked1.get()).isEqualTo(locked2.get());
    }

    @Test
    public void testAcquireAccountAgainIfItWasReleased() {
        String accId1 = "accId1";
        Optional<AdvisoryLockService.Token> locked1 = lockService.acquire(List.of(accId1), 1);
        assertThat(locked1).isNotEmpty();
        assertThat(lockService.acquire(List.of(accId1), 1)).isEmpty();
        lockService.release(locked1.get());
        locked1 = lockService.acquire(List.of(accId1), 1);
        assertThat(locked1).isNotEmpty();
        lockService.release(locked1.get());
    }

    @Test
    public void testThrowExceptionIfTokenIsNotExist() {
        assertThrows(NullPointerException.class, () -> {
            lockService.release(null);
        });
        Exception exp = assertThrows(LockServiceException.class, () -> {
            lockService.release(new AdvisoryLockService.Token("bla-bla"));
        });
        assertThat(exp.getMessage()).startsWith("Cannot release the lock: it DIDN'T acquired all necessary account");
    }
}
