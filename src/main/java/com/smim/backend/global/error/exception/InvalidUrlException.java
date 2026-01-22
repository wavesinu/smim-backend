package com.smim.backend.global.error.exception;

/**
 * URL이 유효하지 않을 때 발생하는 예외
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }

    public InvalidUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
