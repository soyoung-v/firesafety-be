package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// firesafety-ai POST /explain의 SensorEvidence와 1:1 - 원시 시계열 전체가 아니라 해당 진단이 실제로
// 판정에 쓴 frame_id 시점의 스냅샷 값이다(ADR-013). MyBatis가 SELECT 컬럼을 이 필드에 직접 매핑한다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "진단 시점 센서값 스냅샷 (내부용, 외부 API 아님)")
public class AiExplanationSensorEvidenceReq {

    private Double current;
    private Integer arcCount;
    private Double temperature;
    private Double leakageCurrent;
    private Double totalCurrent;
}
