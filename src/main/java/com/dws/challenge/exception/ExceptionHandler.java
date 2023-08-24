package com.dws.challenge.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Handle exceptions thrown by the service layer:
 * LockServiceException maps to HTTP 429 too many requests,in this
 * case client app can't acquire the lock to make account operation.
 * ServiceException is for other error cases.
 */
@ControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler
    public ResponseEntity<ErrorMessage> handleException(ServiceException ex) {
        ErrorMessage error = new ErrorMessage(ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler
    public ResponseEntity<ErrorMessage> handleException(LockServiceException ex) {
        ErrorMessage error = new ErrorMessage(ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }
}
