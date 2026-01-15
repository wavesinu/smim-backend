package com.smim.backend.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "Internal Server Error"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G002", "Invalid Input Value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G003", "Method Not Allowed"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "G004", "Entity Not Found"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "G005", "Access Denied"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Unauthorized Request"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "Invalid Token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Expired Token");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
