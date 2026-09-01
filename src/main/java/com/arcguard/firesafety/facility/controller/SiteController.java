package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.facility.dto.req.SiteCreateReq;
import com.arcguard.firesafety.facility.dto.req.SiteUpdateReq;
import com.arcguard.firesafety.facility.dto.req.SiteWithAdminCreateReq;
import com.arcguard.firesafety.facility.dto.res.SiteCreateRes;
import com.arcguard.firesafety.facility.dto.res.SiteDetailRes;
import com.arcguard.firesafety.facility.dto.res.SiteListRes;
import com.arcguard.firesafety.facility.dto.res.SiteUpdateRes;
import com.arcguard.firesafety.facility.dto.res.SiteWithAdminCreateRes;
import com.arcguard.firesafety.facility.service.SiteRegistrationService;
import com.arcguard.firesafety.facility.service.SiteService;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sites")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-현장", description = "현장 조회, 등록, 수정, 소프트 삭제")
public class SiteController {

    private final SiteService siteService;

    private final SiteRegistrationService siteRegistrationService;

    // 현장 목록 조회 (GET /api/sites)
    // SUPER_ADMIN은 전체, ADMIN/GENERAL은 배정 현장만 조회
    @Operation(summary = "현장 목록 조회", description = "SUPER_ADMIN은 전체, ADMIN/GENERAL은 배정된 활성 현장만 조회한다.")
    @GetMapping
    public ResultResponse<List<SiteListRes>> getSites() {
        List<SiteListRes> sites = siteService.getSites();
        return ResultResponse.success(String.format("%d rows", sites.size()), sites);
    }

    // 현장 등록 (POST /api/sites)
    // SUPER_ADMIN 전용, 등록 이력은 facility_audit_log에 기록
    @Operation(summary = "현장 등록", description = "SUPER_ADMIN 전용. 등록 이력은 facility_audit_log에 기록한다. "
            + "현장명은 활성 현장 기준으로 중복을 막는다(중복 시 409, 삭제된 현장 이름은 재사용 가능).")
    @PostMapping
    public ResultResponse<SiteCreateRes> createSite(@RequestBody SiteCreateReq req) {
        SiteCreateRes site = siteService.createSite(req);
        return ResultResponse.success("현장 등록 성공", site);
    }

    // 현장 + 현장관리자 통합 등록 (POST /api/sites/with-admin)
    // 현장 생성 / ADMIN 계정 생성 / 담당 현장 배정을 한 트랜잭션으로 처리
    @Operation(summary = "현장+현장관리자 통합 등록",
            description = "SUPER_ADMIN 전용. 현장 등록, 현장관리자(ADMIN) 계정 생성, 담당 현장 배정을 하나의 트랜잭션으로 처리한다. "
                    + "현장명 중복(409)/이메일 중복(409)/입력값 오류(400) 등 어느 단계에서 실패해도 현장과 계정이 모두 롤백된다. "
                    + "현장명은 활성 현장 기준으로만 중복을 막는다(삭제된 현장 이름은 재사용 가능). "
                    + "현장/계정 이력은 각각 facility_audit_log, user_audit_log에 기록된다. 인증은 at 쿠키를 사용한다.")
    @PostMapping("/with-admin")
    public ResultResponse<SiteWithAdminCreateRes> createSiteWithAdmin(@RequestBody SiteWithAdminCreateReq req) {
        SiteWithAdminCreateRes result = siteRegistrationService.createSiteWithAdmin(req);
        return ResultResponse.success("현장 및 현장관리자 등록 성공", result);
    }

    // 현장 상세 조회 (GET /api/sites/{siteId})
    // SUPER_ADMIN은 전체, ADMIN/GENERAL은 배정 현장만 조회
    @Operation(summary = "현장 상세 조회",
            description = "SUPER_ADMIN은 모든 활성 현장, ADMIN/GENERAL은 본인에게 배정된 현장만 조회한다. "
                    + "요청 siteId는 신뢰하지 않고 인증된 userId로 user_site를 재조회해 검증한다(미배정 403). "
                    + "삭제된 현장은 404, panelCount는 활성 분전반 수만 집계한다. 인증은 at 쿠키를 사용한다.")
    @GetMapping("/{siteId}")
    public ResultResponse<SiteDetailRes> getSite(@PathVariable Long siteId) {
        SiteDetailRes site = siteService.getSite(siteId);
        return ResultResponse.success("현장 상세 조회 성공", site);
    }

    // 현장 수정 (PUT /api/sites/{siteId})
    // SUPER_ADMIN 전용, 수정 전/후 이력은 facility_audit_log에 기록
    @Operation(summary = "현장 수정", description = "SUPER_ADMIN 전용. 수정 전/후 이력은 facility_audit_log에 기록한다. "
            + "다른 활성 현장과 이름이 겹치면 409를 반환한다.")
    @PutMapping("/{siteId}")
    public ResultResponse<SiteUpdateRes> updateSite(@PathVariable Long siteId, @RequestBody SiteUpdateReq req) {
        SiteUpdateRes site = siteService.updateSite(siteId, req);
        return ResultResponse.success("현장 수정 성공", site);
    }

    // 현장 삭제 (DELETE /api/sites/{siteId})
    // 물리 삭제하지 않고 deleted_at만 기록
    @Operation(summary = "현장 삭제", description = "물리 삭제하지 않고 deleted_at을 기록한다. 일반 활성 목록에서는 하위 설비와 함께 제외된다.")
    @DeleteMapping("/{siteId}")
    public ResultResponse<Void> deleteSite(@PathVariable Long siteId) {
        siteService.deleteSite(siteId);
        return ResultResponse.success("현장 삭제 성공", null);
    }
}
