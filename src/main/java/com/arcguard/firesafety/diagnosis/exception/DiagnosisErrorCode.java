package com.arcguard.firesafety.diagnosis.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DiagnosisErrorCode implements ErrorCode {

    AI_PREDICTION_UNAVAILABLE("DIAGNOSIS-001", "AI 진단 서버를 사용할 수 없습니다", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
