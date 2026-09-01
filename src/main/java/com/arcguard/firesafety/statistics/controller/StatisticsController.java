package com.arcguard.firesafety.statistics.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.statistics.dto.req.StatisticsReq;
import com.arcguard.firesafety.statistics.dto.res.StatisticsSummaryRes;
import com.arcguard.firesafety.statistics.service.StatisticsService;
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
@RequestMapping("/api/statistics")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "통계", description = "기간/현장 기준 통계 조회")
public class StatisticsController {

    private final StatisticsService statisticsService;

    // 기간별 통계 조회 (GET /api/statistics)
    // SUPER_ADMIN은 전체/선택 현장, ADMIN/GENERAL은 담당 현장 범위만 조회
    @Operation(summary = "기간별 통계 조회", description = "경보 상태/유형/소스, 일자별 경보 수, AI 판정, 활성 분전반 상태 통계를 조회한다.")
    @GetMapping
    public ResultResponse<StatisticsSummaryRes> getStatistics(@ModelAttribute StatisticsReq req) {
        StatisticsSummaryRes statistics = statisticsService.getStatistics(req);
        return ResultResponse.success("통계 조회 성공", statistics);
    }
}
