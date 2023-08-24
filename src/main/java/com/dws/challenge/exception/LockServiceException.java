package com.dws.challenge.exception;


/**
 * Throw by AdvisoryLockService
 */
public class LockServiceException extends RuntimeException {
    public LockServiceException(String msg) {
        super(msg);
    }
}
