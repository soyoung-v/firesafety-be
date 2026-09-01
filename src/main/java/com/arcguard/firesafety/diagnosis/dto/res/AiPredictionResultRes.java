package com.arcguard.firesafety.diagnosis.dto.res;

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
    @Schema(description = "아크일 확률(0~1)", example = "0.92")
    private Double proba;
    @Schema(description = "판정값. 0=NORMAL, 1=ARC", example = "1")
    private Integer pred;

    @Schema(description = "AI 서버가 사용한 샘플 개수", example = "60")
    @JsonProperty("n_samples")
    private Integer nSamples;

    @Schema(description = "AI 서버 경고 메시지(선택)", example = "샘플 부족")
    private String warning;
}
