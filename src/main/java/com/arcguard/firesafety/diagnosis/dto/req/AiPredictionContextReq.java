package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "AI 서버 POST /predict 확장 context - 분전반 공통 센서값 시계열 (내부용, 외부 API 아님)")
public class AiPredictionContextReq {

    @Schema(description = "circuit.samples와 같은 개수·프레임 순서로 정렬된 공통 센서값 목록")
    private List<AiPredictionContextSampleReq> samples;
}
