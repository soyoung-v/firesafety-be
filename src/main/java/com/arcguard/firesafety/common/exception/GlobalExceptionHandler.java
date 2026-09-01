package com.arcguard.firesafety.common.exception;

import com.arcguard.firesafety.common.response.ResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// 모든 컨트롤러 예외의 ResultResponse 포맷 통일
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 비즈니스 예외 응답 변환
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultResponse<?>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ResultResponse.error(e.getErrorCode().getMessage()));
    }

    // 요청 DTO 검증 실패 응답 변환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("유효성 검사 실패");

        return ResponseEntity
                .badRequest()
                .body(ResultResponse.error(message));
    }

    // 요청 파라미터 누락/타입 불일치 응답 변환
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ResultResponse<?>> handleInvalidParameterException(Exception e) {
        log.debug("잘못된 요청 파라미터", e);
        return ResponseEntity
                .status(CommonErrorCode.INVALID_PARAMETER.getHttpStatus())
                .body(ResultResponse.error(CommonErrorCode.INVALID_PARAMETER.getMessage()));
    }

    // 미처리 예외 공통 응답 변환
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultResponse<?>> handleException(Exception e) {
        log.error("서버 오류 발생", e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ResultResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
