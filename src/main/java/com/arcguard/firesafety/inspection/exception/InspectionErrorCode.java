package com.arcguard.firesafety.inspection.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InspectionErrorCode implements ErrorCode {

    ITEM_NAME_REQUIRED("INSPECTION-001", "점검 항목명을 입력해주세요", HttpStatus.BAD_REQUEST),
    ITEM_NOT_FOUND("INSPECTION-002", "점검 항목을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    RESULTS_REQUIRED("INSPECTION-003", "점검 결과 목록이 비어있습니다", HttpStatus.BAD_REQUEST),
    INSPECTION_EXPORT_FAILED("INSPECTION-004", "점검 이력 엑셀 파일을 생성할 수 없습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    ITEM_NOT_IN_SITE("INSPECTION-005", "해당 현장의 점검 항목이 아닙니다", HttpStatus.BAD_REQUEST),
    ITEM_IN_USE("INSPECTION-006", "이미 분전반에 적용됐거나 점검 결과에 쓰인 항목은 삭제할 수 없습니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
