package com.arcguard.firesafety.diagnosis.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DiagnosisErrorCode implements ErrorCode {

    AI_PREDICTION_UNAVAILABLE("DIAGNOSIS-001", "AI 진단 서버를 사용할 수 없습니다", HttpStatus.SERVICE_UNAVAILABLE),
    DIAGNOSIS_RESULT_NOT_FOUND("DIAGNOSIS-002", "AI 진단 결과를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    AI_EXPLANATION_DISABLED("DIAGNOSIS-003", "AI 설명 생성 기능을 사용할 수 없습니다", HttpStatus.SERVICE_UNAVAILABLE),
    AI_EXPLANATION_FAILED("DIAGNOSIS-004", "AI 설명 생성에 실패했습니다", HttpStatus.BAD_GATEWAY),
    AI_EXPLANATION_EVIDENCE_UNAVAILABLE("DIAGNOSIS-005", "이 진단 결과는 설명을 생성할 근거 데이터가 없습니다", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
