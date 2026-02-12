package com.smim.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import com.smim.backend.global.common.response.ResultCode;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ResultCode {

    // 공통 에러 (CO: Common)
    INTERNAL_SERVER_ERROR("CO001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("CO002", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("CO003", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("CO004", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    VALIDATION_FAILED("CO005", "입력값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 인증 에러 (AU: Auth)
    UNAUTHORIZED("AU001", "인증되지 않은 요청입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AU002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AU003", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AU004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    UNSUPPORTED_PROVIDER("AU005", "지원하지 않는 소셜 로그인입니다.", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD("AU006", "비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    SOCIAL_ACCOUNT_LOGIN_REQUIRED("AU007", "소셜 로그인으로 가입된 계정입니다.", HttpStatus.UNAUTHORIZED),

    // 사용자 에러 (US: User)
    USER_NOT_FOUND("US001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("US002", "이미 등록된 이메일입니다.", HttpStatus.CONFLICT),

    // 아티클 에러 (AR: Article)
    INVALID_URL("AR001", "유효하지 않은 URL입니다.", HttpStatus.BAD_REQUEST),
    CRAWLING_FAILED("AR002", "크롤링에 실패했습니다.", HttpStatus.BAD_REQUEST),
    ARTICLE_NOT_FOUND("AR003", "아티클을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    EXTRACTION_IN_PROGRESS("AR004", "단어 추출이 진행 중입니다.", HttpStatus.CONFLICT),
    ARTICLE_ACCESS_DENIED("AR005", "해당 아티클에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 단어장 에러 (VB: Vocabulary Book)
    VOCABULARY_BOOK_NOT_FOUND("VB001", "단어장을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DEFAULT_BOOK_CANNOT_BE_DELETED("VB002", "기본 단어장은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_BOOK_NAME("VB003", "이미 존재하는 단어장 이름입니다.", HttpStatus.CONFLICT),
    MAX_BOOKS_EXCEEDED("VB004", "단어장 개수가 최대 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_ENTRY("VB005", "이미 단어장에 존재하는 단어입니다.", HttpStatus.CONFLICT),

    // 사용량 제한 (RL: Rate Limit)
    DAILY_LIMIT_EXCEEDED("RL001", "일일 사용 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
