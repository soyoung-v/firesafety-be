package com.arcguard.firesafety.common.exception;

import lombok.Getter;

// 서비스 계층에서 예상 가능한 비즈니스 실패 전달
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    // 응답 메시지와 HTTP 상태를 ErrorCode 기준으로 고정
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
