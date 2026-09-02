package com.arcguard.firesafety.diagnosis.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// firesafety-ai POST /explain 응답 - 최소 안정 계약(analysisSummary 하나)과 1:1 (내부용, 외부 API 아님)
@Getter
@Setter
@Schema(description = "firesafety-ai POST /explain 응답 (내부용, 외부 API 아님)")
public class AiExplanationRes {

    private String analysisSummary;
}
