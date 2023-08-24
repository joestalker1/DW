package com.dws.challenge.service;

import com.dws.challenge.exception.LockServiceException;
import lombok.Data;

import java.util.Optional;

/**
 * Simple advisory lock used by service layer.
 * If it acquires many accountIds, it makes sorting accountIds in lexicographical order.
 * It's simpler and scalable,than use real lock mutex java object.
 * You may use Redis to implement it.
 */
public interface AdvisoryLockService {
    /**
     * Acquire accountsId, it takes all-or-nothing accountIds,
     * so if some of them is used by other account operation,
     * this service returns Optional.empty.This doesn't throw exception
     * to manage flow, and it makes this operation faster.
     *
     * @param accountId
     * @return
     */
    Optional<Token> acquire(String... accountId);

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
