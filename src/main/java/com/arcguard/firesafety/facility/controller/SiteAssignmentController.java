package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.facility.dto.req.SiteAssignmentSaveReq;
import com.arcguard.firesafety.facility.dto.res.SiteAssignmentRes;
import com.arcguard.firesafety.facility.service.SiteAssignmentService;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "담당현장배정", description = "사용자별 담당 현장 조회, 배정, 해제, 재배정")
public class SiteAssignmentController {

    private final SiteAssignmentService siteAssignmentService;

    // 담당 현장 조회 (GET /api/users/{userId}/site-assignments)
    // 대상 사용자에게 배정된 활성 현장만 반환
    @Operation(summary = "담당 현장 조회", description = "대상 사용자에게 배정된 활성 현장만 반환한다.")
    @GetMapping("/users/{userId}/site-assignments")
    public ResultResponse<List<SiteAssignmentRes>> getSiteAssignments(@PathVariable Long userId) {
        List<SiteAssignmentRes> assignments = siteAssignmentService.getSiteAssignments(userId);
        return ResultResponse.success(String.format("%d rows", assignments.size()), assignments);
    }

    // 담당 현장 저장 (POST /api/users/{userId}/site-assignments)
    // 체크된 현장 목록 전체를 기준으로 신규 배정, 재배정, 해제를 한 번에 처리
    @Operation(summary = "담당 현장 저장", description = "체크된 현장 목록 기준으로 신규 배정, 해제, 재배정을 한 번에 처리한다. 해제는 user_site.deleted_at 기록, 재배정은 deleted_at을 NULL로 되돌린다.")
    @PostMapping("/users/{userId}/site-assignments")
    public ResultResponse<List<SiteAssignmentRes>> saveSiteAssignments(@PathVariable Long userId,
                                                                       @RequestBody SiteAssignmentSaveReq req) {
        List<SiteAssignmentRes> assignments = siteAssignmentService.saveSiteAssignments(userId, req);
        return ResultResponse.success("담당 현장 배정 성공", assignments);
    }
}
