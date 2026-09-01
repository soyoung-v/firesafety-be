package com.arcguard.firesafety.diagnosis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "AI 진단 결과 목록 조회 조건")
public class DiagnosisResultListReq {

    @Schema(description = "페이지 번호. 0부터 시작", example = "0")
    private Integer page;

    @Schema(description = "페이지 크기", example = "20")
    private Integer size;
}
