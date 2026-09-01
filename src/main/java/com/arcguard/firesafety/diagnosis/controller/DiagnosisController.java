package com.arcguard.firesafety.diagnosis.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.diagnosis.dto.req.DiagnosisResultListReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiPredictionTriggerRes;
import com.arcguard.firesafety.diagnosis.dto.res.DiagnosisResultPageRes;
import com.arcguard.firesafety.diagnosis.service.DiagnosisQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/circuits")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "AI진단", description = "회로별 AI 진단 결과 조회")
public class DiagnosisController {

    private final DiagnosisQueryService diagnosisQueryService;

    // 회로 진단결과 조회 (GET /api/circuits/{circuitId}/diagnosis)
    // 회로 접근 권한을 확인한 뒤 최신 AI 판정순으로 반환
    @Operation(summary = "회로 진단결과 조회", description = "회로 접근 권한을 확인한 뒤 최신 AI 판정 이력을 조회한다. AI 판정은 NORMAL/ARC 이진 분류다.")
    @GetMapping("/{circuitId}/diagnosis")
    public ResultResponse<DiagnosisResultPageRes> getDiagnosisResults(@PathVariable Long circuitId,
                                                                      @ModelAttribute DiagnosisResultListReq req) {
        DiagnosisResultPageRes results = diagnosisQueryService.getDiagnosisResults(circuitId, req);
        return ResultResponse.success(String.format("%d rows", results.getContent().size()), results);
    }

    // AI 진단 수동 실행 (POST /api/circuits/{circuitId}/diagnosis/trigger, REQ-102)
    // 요청만 즉시 접수하는 비동기 트리거라 결과는 이 응답에 없다 - 위 조회 API로 재조회해야 한다
    @Operation(summary = "AI 진단 수동 실행", description = "회로에 대한 AI 진단을 즉시 1회 요청한다. 비동기 트리거이며 실제 판정 결과는 회로 진단결과 조회 API로 재조회한다.")
    @PostMapping("/{circuitId}/diagnosis/trigger")
    public ResultResponse<AiPredictionTriggerRes> triggerDiagnosis(@PathVariable Long circuitId) {
        diagnosisQueryService.triggerManualDiagnosis(circuitId);
        return ResultResponse.success("AI 진단 요청 성공", new AiPredictionTriggerRes(true));
    }

}
