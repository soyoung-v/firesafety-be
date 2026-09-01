package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "AI 서버로 보내는 회로 샘플 1건 (내부용, 외부 API 아님)")
public class AiPredictionSampleReq {

    @Schema(description = "전류값(sensor_frame_circuit.current_a)", example = "5.2")
    private BigDecimal am;
    @Schema(description = "회로 카운터(sensor_frame_circuit.arc_counter). 0=정상, 증가 시 아크 의심", example = "0")
    private Integer count;
}
