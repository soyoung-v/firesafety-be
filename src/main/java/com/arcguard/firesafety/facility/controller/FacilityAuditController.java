package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.facility.dto.req.FacilityAuditLogSearchReq;
import com.arcguard.firesafety.facility.dto.res.FacilityAuditLogPageRes;
import com.arcguard.firesafety.facility.service.FacilityAuditService;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
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
@RequestMapping("/api/facilities/audit-logs")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-감사로그", description = "현장, 분전반, 회로 변경 이력 조회")
public class FacilityAuditController {

    private final FacilityAuditService facilityAuditService;

    // 설비 변경 이력 조회 (GET /api/facilities/audit-logs)
    // SUPER_ADMIN 전용, 현장/분전반/회로 변경 이력을 필터링해서 조회
    @Operation(summary = "설비 변경 이력 조회", description = "SUPER_ADMIN 전용. 현장/분전반/회로 등록, 수정, 삭제 이력을 필터링해서 조회한다.")
    @GetMapping
    public ResultResponse<FacilityAuditLogPageRes> getAuditLogs(@ModelAttribute FacilityAuditLogSearchReq req) {
        FacilityAuditLogPageRes auditLogs = facilityAuditService.getAuditLogs(req);
        return ResultResponse.success(String.format("%d rows", auditLogs.getContent().size()), auditLogs);
    }
}
