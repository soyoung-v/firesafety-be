package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// firesafety-ai POST /explain의 PredictionEvidence와 1:1 - 이미 저장된 AI 판정 결과를 그대로 담는다.
// LLM은 이 값을 재계산하지 않는다(내부용, 외부 API 아님).
@Getter
@AllArgsConstructor
@Schema(description = "이미 확정된 ML 판정 결과 (내부용, 외부 API 아님)")
public class AiExplanationPredictionReq {

    private int pred;
    private double proba;
    private String riskLevel;
    private Double riskScore;
    private Boolean anomaly;
    private Double anomalyScore;
    private Double predictedCurrent;
}
