package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteCreateReq;
import com.arcguard.firesafety.facility.dto.req.SiteUpdateReq;
import com.arcguard.firesafety.facility.dto.res.SiteCreateRes;
import com.arcguard.firesafety.facility.dto.res.SiteDetailRes;
import com.arcguard.firesafety.facility.dto.res.SiteListRes;
import com.arcguard.firesafety.facility.dto.res.SiteUpdateRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.FacilityAuditAction;
import com.arcguard.firesafety.facility.model.FacilityAuditLog;
import com.arcguard.firesafety.facility.model.FacilityAuditTargetType;
import com.arcguard.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteService {

    // site, user_site, facility_audit_log 테이블 접근
    private final SiteMapper siteMapper;

    // 현장 상세의 분전반 수 집계
    private final PanelMapper panelMapper;

    // 감사 로그 after JSON 변환
    private final ObjectMapper objectMapper;

    // 현장 등록
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 현장 저장 → 4. 감사 로그 저장
    @Transactional
    public SiteCreateRes createSite(SiteCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);
        validateCreateRequest(req);
        validateDuplicatedName(req.getName(), null);

        Site site = buildSiteForCreate(req);
        siteMapper.insertSite(site);

        Site savedSite = findActiveSite(site.getSiteId());
        insertFacilityAuditLog(savedSite, actor.getUserId(), FacilityAuditAction.CREATE, null, toAuditJson(savedSite));

        return SiteCreateRes.from(savedSite);
    }

    // 현장 목록 조회
    // 1. 현재 사용자 확인 → 2. 역할 확인 → 3. 역할별 조회 범위 적용
    @Transactional(readOnly = true)
    public List<SiteListRes> getSites() {
        UserPrincipal actor = getCurrentUser();
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        List<Site> sites;
        if (actorRole == UserRole.SUPER_ADMIN) {
            sites = siteMapper.findActiveSites();
        } else {
            // ADMIN/GENERAL은 user_site에 배정된 활성 현장만 조회
            sites = siteMapper.findActiveSitesByUserId(actor.getUserId());
        }

        return sites.stream()
                .map(SiteListRes::from)
                .toList();
    }

    // 현장 상세 조회
    // 1. 현재 사용자 확인 → 2. 활성 현장 조회 → 3. 현장 접근 권한 확인 → 4. 분전반 수 집계
    @Transactional(readOnly = true)
    public SiteDetailRes getSite(Long siteId) {
        UserPrincipal actor = getCurrentUser();
        validateSiteId(siteId);

        Site site = findActiveSite(siteId);
        validateSiteAccess(actor, siteId);

        return SiteDetailRes.from(site, panelMapper.countActivePanelsBySiteId(siteId));
    }

    // 현장 수정
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 현장 조회 → 4. 수정 → 5. 감사 로그 저장
    @Transactional
    public SiteUpdateRes updateSite(Long siteId, SiteUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);
        validateUpdateRequest(siteId, req);
        validateDuplicatedName(req.getName(), siteId);

        Site site = findActiveSite(siteId);
        String beforeData = toAuditJson(site);

        applyUpdate(site, req);
        int updatedRows = siteMapper.updateSite(site);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        Site updatedSite = findActiveSite(siteId);
        String afterData = toAuditJson(updatedSite);
        if (!beforeData.equals(afterData)) {
            insertFacilityAuditLog(updatedSite, actor.getUserId(), FacilityAuditAction.UPDATE, beforeData, afterData);
        }

        return SiteUpdateRes.from(updatedSite);
    }

    // 현장 소프트 삭제
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 현장 조회 → 4. deleted_at 기록 → 5. 감사 로그 저장
    @Transactional
    public void deleteSite(Long siteId) {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);
        validateSiteId(siteId);

        Site site = findActiveSite(siteId);
        String beforeData = toAuditJson(site);

        int updatedRows = siteMapper.softDeleteSite(siteId);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        // 삭제 후 일반 조회에서 빠지므로 감사 로그용 상태는 메모리에서 반영
        site.setDeletedAt(LocalDateTime.now());
        site.setUpdatedAt(LocalDateTime.now());
        insertFacilityAuditLog(site, actor.getUserId(), FacilityAuditAction.DELETE, beforeData, toAuditJson(site));
    }

    // 등록 요청값 확인
    private void validateCreateRequest(SiteCreateReq req) {
        if (req == null || !StringUtils.hasText(req.getName())) {
            throw new BusinessException(FacilityErrorCode.SITE_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(req.getAddress())) {
            throw new BusinessException(FacilityErrorCode.SITE_ADDRESS_REQUIRED);
        }
    }

    // 수정 요청값 확인
    private void validateUpdateRequest(Long siteId, SiteUpdateReq req) {
        validateSiteId(siteId);
        if (req == null || !StringUtils.hasText(req.getName())) {
            throw new BusinessException(FacilityErrorCode.SITE_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(req.getAddress())) {
            throw new BusinessException(FacilityErrorCode.SITE_ADDRESS_REQUIRED);
        }
    }

    // 현장 ID 확인
    private void validateSiteId(Long siteId) {
        if (siteId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 등록용 Site 객체 생성
    private Site buildSiteForCreate(SiteCreateReq req) {
        Site site = new Site();
        site.setName(req.getName());
        site.setAddress(req.getAddress());
        site.setAddressDetail(req.getAddressDetail());
        site.setZipCode(req.getZipCode());
        return site;
    }

    // 수정값을 Site 객체에 반영
    private void applyUpdate(Site site, SiteUpdateReq req) {
        site.setName(req.getName());
        site.setAddress(req.getAddress());
        site.setAddressDetail(req.getAddressDetail());
        site.setZipCode(req.getZipCode());
    }

    // 등록 직후 활성 현장 재조회
    private Site findActiveSite(Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }
        return site;
    }

    // 현장명 중복 확인
    // site.name에는 DB UNIQUE 제약이 없다 — 삭제된 현장 이름은 재등록할 수 있어야 해서 활성 현장끼리만 서비스에서 막는다
    private void validateDuplicatedName(String name, Long excludeSiteId) {
        if (siteMapper.existsActiveSiteByName(name, excludeSiteId)) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_SITE_NAME);
        }
    }

    // 현장 접근 권한 확인
    private void validateSiteAccess(UserPrincipal actor, Long siteId) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }

        // 요청으로 넘어온 siteId를 그대로 믿지 않고 인증된 userId 기준으로 배정 여부를 재조회
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
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

    // 설비 감사 로그 저장
    private void insertFacilityAuditLog(Site site,
                                        Long actorUserId,
                                        FacilityAuditAction action,
                                        String beforeData,
                                        String afterData) {
        FacilityAuditLog auditLog = new FacilityAuditLog();
        auditLog.setTargetType(FacilityAuditTargetType.SITE);
        auditLog.setTargetId(site.getSiteId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        siteMapper.insertFacilityAuditLog(auditLog);
    }

    // 감사 로그 before/after JSON 생성
    private String toAuditJson(Site site) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("siteId", site.getSiteId());
            auditData.put("name", site.getName());
            auditData.put("address", site.getAddress());
            auditData.put("createdAt", site.getCreatedAt());
            // updatedAt은 매 UPDATE 쿼리마다 CURRENT_TIMESTAMP로 무조건 갱신되는 컬럼이라
            // 감사 로그 diff 비교에 넣으면 실제 값 변경이 없어도 항상 다르게 나온다 — 비교 대상에서 제외
            auditData.put("deletedAt", site.getDeletedAt());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("설비 감사 로그 직렬화 실패", e);
        }
    }
}
