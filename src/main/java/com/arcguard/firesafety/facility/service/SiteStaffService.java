package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteManagedUserListReq;
import com.arcguard.firesafety.facility.dto.res.SiteManagedUserPageRes;
import com.arcguard.firesafety.facility.dto.res.SiteManagedUserRes;
import com.arcguard.firesafety.facility.dto.res.SiteStaffContactRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteStaffService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    // SUPER_ADMIN/ADMIN 관리 목록은 현장에 배정된 ADMIN/GENERAL을 함께 조회한다.
    private static final List<String> MANAGED_USER_ROLES =
            List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name());

    // 연락망은 같은 현장에서 실제로 연락할 대상인 ADMIN/GENERAL을 함께 노출
    private static final List<String> STAFF_CONTACT_ROLES = List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name());

    // site, user_site 접근 (담당 현장 범위 재검증)
    private final SiteMapper siteMapper;

    // user 조회는 auth 도메인 mapper를 통해서만 수행
    private final AuthMapper authMapper;

    // 현장 담당 직원 목록 조회(SCR-404) — keyword/role 필터 + page/size 페이징
    // 1. 현재 사용자 확인 → 2. 현장 접근 권한 재조회 → 3. 필터/페이징 계산 → 4. 활성 ADMIN/GENERAL 조회
    @Transactional(readOnly = true)
    public SiteManagedUserPageRes getManagedUsers(Long siteId, SiteManagedUserListReq req) {
        UserPrincipal actor = getCurrentUser();
        validateSiteId(siteId);
        findAccessibleSite(actor, siteId);

        SiteManagedUserListReq searchReq = req == null ? new SiteManagedUserListReq() : req;
        List<String> roles = searchReq.getRole() == null ? MANAGED_USER_ROLES : List.of(searchReq.getRole().name());
        String keyword = normalizeKeyword(searchReq.getKeyword());
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;

        List<SiteManagedUserRes> content = authMapper
                .findActiveSiteUsersByRolesPaged(siteId, roles, keyword, size, offset).stream()
                .map(SiteManagedUserRes::from)
                .toList();
        long totalElements = authMapper.countActiveSiteUsersByRoles(siteId, roles, keyword);

        return new SiteManagedUserPageRes(content, totalElements);
    }

    // 공백만 입력한 검색어는 필터 없음으로 처리
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(SiteManagedUserListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(SiteManagedUserListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 같은 현장 직원 연락망 조회
    // 1. 현재 사용자 확인 → 2. 현장 접근 권한 재조회 → 3. 활성 ADMIN/GENERAL 조회 → 4. 연락용 최소 필드만 반환
    @Transactional(readOnly = true)
    public List<SiteStaffContactRes> getStaffContacts(Long siteId) {
        UserPrincipal actor = getCurrentUser();
        validateSiteId(siteId);

        Site site = findAccessibleSite(actor, siteId);

        List<User> staff = authMapper.findActiveSiteUsersByRoles(siteId, STAFF_CONTACT_ROLES);
        return staff.stream()
                .map(user -> SiteStaffContactRes.from(user, site))
                .toList();
    }

    // 현장 ID 확인
    private void validateSiteId(Long siteId) {
        if (siteId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 활성 현장 조회 + 담당 현장 범위 확인
    private Site findAccessibleSite(UserPrincipal actor, Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return site;
        }

        // 요청 siteId를 신뢰하지 않고 인증된 userId 기준으로 user_site를 재조회
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
        return site;
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
