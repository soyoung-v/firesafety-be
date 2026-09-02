package com.arcguard.firesafety.diagnosis.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 신규 Risk Classifier의 3단계 판정. 기존 Verdict(NORMAL/ARC, 아크 전용 이진 분류)와는
// 별개 개념이라 같은 enum을 재사용하지 않는다 (ADR-002, firesafety-ai).
@Getter
@RequiredArgsConstructor
public enum RiskLevel {

    NORMAL("정상"),
    WARNING("주의"),
    DANGER("위험");

    private final String label;
}
