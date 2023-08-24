package com.dws.challenge.service;

import com.dws.challenge.exception.LockServiceException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple implementation of advisory locks. it needs to add ttl to prevent forever locks.
 */
@Service
@Validated
@Slf4j
public class AdvisoryLockServiceImp implements AdvisoryLockService {

    //must be part of the configuration
    private static long START_LOCK_PAUSE_IN_MILLIS = 100l;

    private static String KEY_SEP = ":";
    private final Set<String> acquiredAccounts = new HashSet<>();

    @Override
    public Optional<Token> acquire(@NonNull List<String> accountId, int retryTimes) {
        if (accountId.size() == 0) {
            return Optional.empty();
        }
        List<String> sortedAccountIds = new ArrayList<>();
        sortedAccountIds.addAll(accountId);
        //sort accountId to get acquiring lock in the specific order
        sortedAccountIds.sort(Comparator.naturalOrder());
        return tryAcquireLockFewTimes(sortedAccountIds, START_LOCK_PAUSE_IN_MILLIS, retryTimes);
    }

    private Boolean canAcquireAll(List<String> accIds) {
        synchronized (acquiredAccounts) {
            //if any of required accountId has been already acquired, it returns false
            if (accIds.stream().filter(accId -> acquiredAccounts.contains(accId)).count() > 0) {
                return false;
            }
            acquiredAccounts.addAll(accIds);
        }
        return true;
    }

    private String createKey(List<String> accIds) {
        return accIds.stream().collect(Collectors.joining(KEY_SEP));
    }

    private Optional<Token> tryAcquireLockFewTimes(List<String> accountIds, long pause, int times) {
        if (times == 0) {
            return Optional.empty();
        }
        if (canAcquireAll(accountIds))
            return Optional.of(new Token(createKey(accountIds)));
        else {
            try {
                Thread.sleep(pause);
            } catch (InterruptedException ignored) {
            }
            return tryAcquireLockFewTimes(accountIds, pause * 2, times - 1);
        }
    }

    @Override
    public void release(@NonNull Token token) throws LockServiceException {
        synchronized (acquiredAccounts) {
            String parsedAccIds[] = token.getToken().split(KEY_SEP);
            if (parsedAccIds.length == 0)
                throw new LockServiceException("Cannot release the lock: token is invalid,it should be non empty.");
            if (Arrays.stream(parsedAccIds).allMatch(acc -> acquiredAccounts.contains(acc))) {
                acquiredAccounts.removeAll(List.of(parsedAccIds));
            } else {
                throw new LockServiceException("Cannot release the lock: it DIDN'T acquired all necessary accounts: " + Arrays.stream(parsedAccIds).collect(Collectors.joining(",")));
            }
        }
    }


}
