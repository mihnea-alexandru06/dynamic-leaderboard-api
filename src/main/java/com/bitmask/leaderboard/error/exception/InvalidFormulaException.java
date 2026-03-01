package com.bitmask.leaderboard.error.exception;

public class InvalidFormulaException extends RuntimeException {
    public InvalidFormulaException(String message) {
        super(message);
    }

    public InvalidFormulaException(String message, Throwable cause) {
        super(message, cause);
    }
}
