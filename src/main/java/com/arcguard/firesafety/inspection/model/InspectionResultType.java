package com.arcguard.firesafety.inspection.model;

import lombok.Getter;

// 점검 항목 1개의 결과. DB에는 이 이름 그대로 저장하고 한글은 label로만 보여준다 (Enum 처리 규칙)
@Getter
public enum InspectionResultType {

    NORMAL("정상"),
    ABNORMAL("이상"),
    UNCHECKED("미확인");

    private final String label;

    InspectionResultType(String label) {
        this.label = label;
    }
}
