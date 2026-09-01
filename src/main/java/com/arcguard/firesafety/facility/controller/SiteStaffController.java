package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.facility.dto.req.SiteManagedUserListReq;
import com.arcguard.firesafety.facility.dto.res.SiteManagedUserPageRes;
import com.arcguard.firesafety.facility.dto.res.SiteStaffContactRes;
import com.arcguard.firesafety.facility.service.SiteStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sites")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-현장직원", description = "현장 담당 직원 목록, 같은 현장 직원 연락망")
public class SiteStaffController {

    private final SiteStaffService siteStaffService;

    // 현장 담당 직원 목록 조회 (GET /api/sites/{siteId}/managed-users)
    // 역할 공통 직원 목록 조회. 권한별 차이는 조회 데이터가 아니라 관리 API 권한에서 구분한다.
    // keyword(이름/이메일/전화번호 부분일치)/role 필터 + page/size 페이징 지원
    @Operation(summary = "현장 담당 직원 목록 조회",
            description = "SUPER_ADMIN은 모든 활성 현장, ADMIN/GENERAL은 본인에게 배정된 현장만 조회할 수 있다. "
                    + "요청 siteId는 신뢰하지 않고 인증된 userId로 user_site를 재조회해 배정 여부를 검증한다. "
                    + "해당 현장에 배정된 활성 ADMIN/GENERAL을 반환하고 SUPER_ADMIN은 제외한다. "
                    + "keyword(이름/이메일/전화번호 부분일치)/role 필터와 page/size 페이징을 지원한다. "
                    + "소프트 삭제된 사용자/배정/현장은 모두 제외된다. 인증은 at 쿠키를 사용한다.")
    @GetMapping("/{siteId}/managed-users")
    public ResultResponse<SiteManagedUserPageRes> getManagedUsers(@PathVariable Long siteId,
                                                                    @ModelAttribute SiteManagedUserListReq req) {
        SiteManagedUserPageRes users = siteStaffService.getManagedUsers(siteId, req);
        return ResultResponse.success(String.format("%d rows", users.getContent().size()), users);
    }

    // 같은 현장 직원 연락망 조회 (GET /api/sites/{siteId}/staff-contacts)
    // GENERAL도 사용하는 화면이라 이메일 등 관리용 필드는 응답에 포함하지 않음
    @Operation(summary = "같은 현장 직원 연락망 조회",
            description = "ADMIN/GENERAL은 본인에게 배정된 현장만, SUPER_ADMIN은 모든 활성 현장을 조회할 수 있다(미배정 403). "
                    + "요청 siteId는 신뢰하지 않고 인증된 userId로 user_site를 재조회해 검증한다. "
                    + "응답은 연락에 필요한 최소 정보(userId, name, phone, role, siteId, siteName)만 포함하며 이메일/생성자/삭제 정보는 제공하지 않는다. "
                    + "소프트 삭제된 사용자/배정/현장은 모두 제외된다. 인증은 at 쿠키를 사용한다.")
    @GetMapping("/{siteId}/staff-contacts")
    public ResultResponse<List<SiteStaffContactRes>> getStaffContacts(@PathVariable Long siteId) {
        List<SiteStaffContactRes> contacts = siteStaffService.getStaffContacts(siteId);
        return ResultResponse.success(String.format("%d rows", contacts.size()), contacts);
    }
}
