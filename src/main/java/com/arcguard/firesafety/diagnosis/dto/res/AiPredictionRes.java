package com.arcguard.firesafety.diagnosis.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "AI 서버 POST /predict 응답 (내부용, 외부 API 아님)")
public class AiPredictionRes {

    @Schema(description = "분전반 장비번호", example = "00099")
    @JsonProperty("m_no")
    private String mNo;

    @Schema(description = "AI 서버가 판정에 사용한 기준값", example = "0.5")
    private Double threshold;
    @Schema(description = "회로별 판정 결과 목록")
    private List<AiPredictionResultRes> results;
}
