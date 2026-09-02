package com.arcguard.firesafety.diagnosis.dto.res;

import com.arcguard.firesafety.diagnosis.model.RiskLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "AI 서버 회로별 판정 결과 1건 (내부용, 외부 API 아님)")
public class AiPredictionResultRes {

    @Schema(description = "회로 번호", example = "1")
    private Integer circuit;
    @Schema(description = "아크일 확률(0~1). 기존 Legacy ARC 계약, 항상 값이 있다", example = "0.92")
    private Double proba;
    @Schema(description = "판정값. 0=NORMAL, 1=ARC. 기존 Legacy ARC 계약, 항상 값이 있다", example = "1")
    private Integer pred;

    @Schema(description = "AI 서버가 사용한 샘플 개수", example = "60")
    @JsonProperty("n_samples")
    private Integer nSamples;

    @Schema(description = "AI 서버 경고 메시지(선택)", example = "샘플 부족")
    private String warning;

    // 신규 확장 필드 - request에 context가 없거나 불충분하면 AI 서버가 null로 내려준다(firesafety-ai
    // .agent-docs/api-contract.md). DANGER/anomaly=true를 기존 ARC 판정으로 강제 변환하지 않는다(ADR-002).
    @Schema(description = "신규 위험도 3단계(NORMAL/WARNING/DANGER). context 없으면 null", example = "NORMAL")
    private RiskLevel riskLevel;

    @Schema(description = "신규 위험도 점수(0~1, 화재 발생 확률 아님). context 없으면 null", example = "0.11")
    private Double riskScore;

    @Schema(description = "정상 패턴 이탈 여부(비지도 판정). context 없으면 null", example = "false")
    private Boolean anomaly;

    @Schema(description = "이상치 점수(0~1, 높을수록 정상과 다름). context 없으면 null", example = "0.43")
    private Double anomalyScore;

    @Schema(description = "다음 sample의 예상 전류값(A). current 이력만 있으면 계산되므로 대체로 항상 값이 있다", example = "5.19")
    private Double predictedCurrent;
}
