package com.smim.backend.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smim.backend.global.error.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @JsonProperty("isSuccess")
    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean isSuccess, ResultCode resultCode, String message, T data) {
        this.isSuccess = isSuccess;
        this.code = resultCode.getCode();
        this.message = message;
        this.data = data;
    }

    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), data);
    }

    /**
     * 성공 응답 (데이터 없음)
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), null);
    }

    /**
     * 실패 응답 (기본 메시지 사용)
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode, errorCode.getMessage(), null);
    }

    /**
     * 실패 응답 (커스텀 메시지 사용)
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }
}
