package com.arcguard.firesafety.diagnosis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiDiagnosisResult {

    private Long resultId;
    private Long circuitId;
    private Long frameId;
    private Verdict verdict;
    private Float confidence;
    private Integer nSamples;
    private String warning;
    private DiagnosisTriggerType triggerType;
    private LocalDateTime diagnosedAt;

    // 신규 확장 결과 - verdict/confidence(기존 ARC 판정)와는 별개 의미. context 없으면 전부 NULL로 저장된다.
    private RiskLevel riskLevel;
    private Float riskScore;
    private Boolean anomaly;
    private Float anomalyScore;
    private Float predictedCurrent;
}
