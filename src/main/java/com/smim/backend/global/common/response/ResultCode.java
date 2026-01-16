package com.smim.backend.global.common.response;

public interface ResultCode {

    String getCode();

    String getMessage();

    org.springframework.http.HttpStatus getHttpStatus();
}
