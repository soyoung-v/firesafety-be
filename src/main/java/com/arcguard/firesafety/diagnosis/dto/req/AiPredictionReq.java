package com.arcguard.firesafety.diagnosis.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "AI 서버 POST /predict 요청 (내부용, 외부 API 아님)")
public class AiPredictionReq {

    @Schema(description = "분전반 장비번호", example = "00099")
    @JsonProperty("m_no")
    private String mNo;

    @Schema(description = "회로별 데이터 목록. 최대 10회로")
    private List<AiPredictionCircuitReq> circuits;
}
