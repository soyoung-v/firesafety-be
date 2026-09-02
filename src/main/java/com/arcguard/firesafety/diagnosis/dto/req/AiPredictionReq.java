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

    // 신규 확장(optional). null이면 AI 서버가 riskLevel/riskScore/anomaly/anomalyScore를 계산하지 않는다.
    @Schema(description = "분전반 공통 센서값 확장(optional). 없으면 AI 서버가 Risk/Anomaly 결과를 계산하지 않는다")
    private AiPredictionContextReq context;

    // 기존 2-args 생성자 호출부(레거시 ARC 전용 흐름)와의 호환을 위해 유지 - context 없이 생성
    public AiPredictionReq(String mNo, List<AiPredictionCircuitReq> circuits) {
        this(mNo, circuits, null);
    }
}
