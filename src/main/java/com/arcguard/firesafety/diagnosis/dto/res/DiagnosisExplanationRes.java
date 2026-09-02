package com.arcguard.firesafety.diagnosis.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// POST /api/circuits/{circuitId}/diagnosis/{resultId}/explanation 응답
@Getter
@AllArgsConstructor
@Schema(description = "AI 진단 결과에 대한 LLM 설명")
public class DiagnosisExplanationRes {

    @Schema(description = "ML 판정 결과에 대한 자연어 설명. LLM이 위험도를 새로 판정하지 않으며, "
            + "이미 계산된 verdict/riskLevel 등을 설명만 한다", example = "최근 전류 증가와 이상 패턴이 함께 감지되었습니다.")
    private String analysisSummary;
}
