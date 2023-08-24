package com.dws.challenge.exception;

import lombok.Data;

/**
 * Show the exception message when handle exception for REST endpoints.
 */
@Data
public class ErrorMessage {
    private String msg;

    public ErrorMessage(String msg) {
        this.msg = msg;
    }
}
