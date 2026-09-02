package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// firesafety-ai POST /explain 요청 - 실제 Pydantic ExplainRequest와 1:1(circuit/prediction/sensorEvidence).
// trendEvidence는 optional 필드라 이번 Phase에서는 보내지 않는다(ADR-013 - 원본 window를 정확히
// 재구성할 수 없는 값을 근사해서 만들지 않는다). synthetic 정답값(scenario/risk_level/run_id/aerror
// 등)은 이 DTO에 필드 자체가 없어 애초에 보낼 수 없다(내부용, 외부 API 아님).
@Getter
@AllArgsConstructor
@Schema(description = "firesafety-ai POST /explain 요청 (내부용, 외부 API 아님)")
public class AiExplanationReq {

    @Schema(description = "회로 번호(channel_no, 1~10)", example = "1")
    private int circuit;

    private AiExplanationPredictionReq prediction;

    private AiExplanationSensorEvidenceReq sensorEvidence;
}
