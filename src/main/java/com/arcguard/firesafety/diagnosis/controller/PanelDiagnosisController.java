package com.arcguard.firesafety.diagnosis.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.diagnosis.dto.res.PanelDiagnosisSummaryRes;
import com.arcguard.firesafety.diagnosis.service.DiagnosisQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/panels")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "AI진단", description = "분전반/회로별 AI 진단 결과 조회")
public class PanelDiagnosisController {

    private final DiagnosisQueryService diagnosisQueryService;

    @Operation(summary = "분전반 AI 진단 현황 조회", description = "분전반의 최근 AI 판정, 최근 24시간 판정 수, 자동 진단 샘플 준비 상태를 조회한다.")
    @GetMapping("/{panelId}/diagnosis/summary")
    public ResultResponse<PanelDiagnosisSummaryRes> getPanelDiagnosisSummary(@PathVariable Long panelId) {
        PanelDiagnosisSummaryRes summary = diagnosisQueryService.getPanelDiagnosisSummary(panelId);
        return ResultResponse.success("AI 진단 현황 조회 성공", summary);
    }
}
