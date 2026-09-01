package com.arcguard.firesafety.diagnosis.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "AI 진단 수동 실행 응답 - 요청만 접수됐다는 뜻이고 실제 판정 결과는 아니다")
public class AiPredictionTriggerRes {

    @Schema(description = "요청 접수 여부. 결과는 회로 진단결과 조회 API로 재조회해야 한다", example = "true")
    private boolean requested;
}
