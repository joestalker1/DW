package com.dws.challenge.service;

import com.dws.challenge.exception.LockServiceException;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * Simple advisory lock used by service layer.
 * If it acquires many accountIds, it makes sorting accountIds in lexicographical order.
 * It's simpler and scalable,than use real mutex in Java.
 * You may use Redis or Zookeeper to implement it.
 */
public interface AdvisoryLockService {
    /**
     * Acquire accountsIds, it does take all-or-nothing accountIds,
     * so if some of them is used by other account operation,
     * This service try to get lock it with exponential retry,if it can't do it at once.
     *
     * @param accountId
     * @return
     */
    Optional<Token> acquire(List<String> accountId, int retryTimes);

    /**
     * Release lock if it's known otherwise throws an exception.
     *
     * @param token
     * @throws LockServiceException
     */
    void release(Token token) throws LockServiceException;

    /**
     * If locked is acquired, it contains unique token.
     */
    @Data
    class Token {
        private final String token;

        Token(String token) {
            this.token = token;
        }
    }
}
