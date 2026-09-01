package com.arcguard.firesafety.monitoring.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.monitoring.dto.req.DashboardSummaryReq;
import com.arcguard.firesafety.monitoring.dto.res.DashboardSummaryRes;
import com.arcguard.firesafety.monitoring.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "관제화면", description = "대시보드 요약 및 실시간 갱신용 조회")
public class DashboardController {

    private final DashboardService dashboardService;

    // 대시보드 요약 조회 (GET /api/dashboard/summary)
    // SUPER_ADMIN은 전체 또는 선택 현장, ADMIN/GENERAL은 담당 현장 기준으로 집계
    @Operation(summary = "대시보드 요약 조회", description = "SUPER_ADMIN은 전체 또는 선택 현장, ADMIN/GENERAL은 담당 현장 기준으로 분전반 상태와 경보 수를 집계한다.")
    @GetMapping("/summary")
    public ResultResponse<DashboardSummaryRes> getSummary(@ModelAttribute DashboardSummaryReq req) {
        DashboardSummaryRes summary = dashboardService.getSummary(req);
        return ResultResponse.success("대시보드 요약 조회 성공", summary);
    }
}
