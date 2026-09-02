package com.arcguard.firesafety.diagnosis.dto.res;

import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.RiskLevel;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PanelDiagnosisRecentRes {

    private Long resultId;
    private Long circuitId;
    private Integer channelNo;
    private Verdict verdict;
    private Float confidence;
    private Integer nSamples;
    private String warning;
    private DiagnosisTriggerType triggerType;
    private LocalDateTime diagnosedAt;

    // 신규 확장 결과 - verdict/confidence(기존 ARC 판정)와는 별개 의미
    private RiskLevel riskLevel;
    private Float riskScore;
    private Boolean anomaly;
    private Float anomalyScore;
    private Float predictedCurrent;

    // Phase 11: LLM 설명 캐시 - 사용자가 설명 생성을 요청하기 전까지는 null(자동 생성 안 함)
    private String analysisSummary;
}
