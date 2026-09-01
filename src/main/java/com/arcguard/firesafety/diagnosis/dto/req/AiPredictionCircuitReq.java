package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "AI 서버로 보내는 회로별 요청 (내부용, 외부 API 아님)")
public class AiPredictionCircuitReq {

    @Schema(description = "회로 번호(circuit.channel_no)", example = "1")
    private Integer circuit;
    @Schema(description = "회로 샘플 목록. 최소 30개 이상")
    private List<AiPredictionSampleReq> samples;
}
