package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.auth.dto.req.UserCreateReq;
import com.arcguard.firesafety.auth.dto.res.UserCreateRes;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.auth.service.UserService;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteAssignmentSaveReq;
import com.arcguard.firesafety.facility.dto.req.SiteCreateReq;
import com.arcguard.firesafety.facility.dto.req.SiteWithAdminCreateReq;
import com.arcguard.firesafety.facility.dto.res.SiteAssignmentRes;
import com.arcguard.firesafety.facility.dto.res.SiteCreateRes;
import com.arcguard.firesafety.facility.dto.res.SiteWithAdminCreateRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 현장 등록 + 현장관리자 계정 생성 + 담당 현장 배정을 하나의 트랜잭션으로 묶는 orchestration 서비스.
// facility가 auth를 호출하는 방향은 기존 SiteAssignmentService와 동일하고, auth는 facility를 참조하지 않아 순환 의존이 생기지 않는다.
@Service
@RequiredArgsConstructor
public class SiteRegistrationService {

    // 현장 등록과 현장명 중복 검증, 현장 감사 로그
    private final SiteService siteService;

    // ADMIN 계정 생성과 사용자 감사 로그 (auth 도메인 Application Service)
    private final UserService userService;

    // user_site 배정
    private final SiteAssignmentService siteAssignmentService;

    // 현장 + 현장관리자 통합 등록
    // 1. SUPER_ADMIN 확인 → 2. 이메일 중복 확인 → 3. 현장 등록 → 4. ADMIN 계정 등록 → 5. 담당 현장 배정
    // 현장명 중복/이메일 중복/배정 실패 등 어느 단계에서 실패해도 이 트랜잭션 전체가 롤백된다.
    @Transactional
    public SiteWithAdminCreateRes createSiteWithAdmin(SiteWithAdminCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);
        validateRequest(req);

        // 계정 이메일 중복은 현장 INSERT 전에 먼저 확인 — 롤백되더라도 site AUTO_INCREMENT를 헛되이 소모하지 않게
        if (userService.checkEmailDuplicate(req.getAdminEmail()).isDuplicate()) {
            throw new BusinessException(AuthErrorCode.DUPLICATED_EMAIL);
        }

        SiteCreateRes site = siteService.createSite(
                new SiteCreateReq(req.getName(), req.getAddress(), req.getAddressDetail(), req.getZipCode()));

        UserCreateRes admin = userService.createUser(new UserCreateReq(
                req.getAdminEmail(),
                req.getAdminPassword(),
                req.getAdminName(),
                req.getAdminPhone(),
                UserRole.ADMIN));

        SiteAssignmentSaveReq assignmentReq = new SiteAssignmentSaveReq();
        assignmentReq.setSiteIds(List.of(site.getSiteId()));
        List<SiteAssignmentRes> assignments =
                siteAssignmentService.saveSiteAssignments(admin.getUserId(), assignmentReq);

        return new SiteWithAdminCreateRes(
                site.getSiteId(),
                site.getName(),
                admin.getUserId(),
                admin.getName(),
                admin.getEmail(),
                assignments.stream().map(SiteAssignmentRes::getSiteId).toList());
    }

    // 통합 등록 요청값 확인. 현장/계정 각각의 상세 검증은 SiteService/UserService가 그대로 담당
    private void validateRequest(SiteWithAdminCreateReq req) {
        if (req == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NAME_REQUIRED);
        }
    }

    // SUPER_ADMIN 권한 확인
    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }
}
