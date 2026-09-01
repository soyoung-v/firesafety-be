package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Site;
import com.arcguard.firesafety.monitoring.dto.req.DashboardSummaryReq;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.monitoring.dto.res.DashboardPanelRes;
import com.arcguard.firesafety.monitoring.dto.res.DashboardSummaryRes;
import com.arcguard.firesafety.monitoring.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final SiteMapper siteMapper;

    // 대시보드 요약 조회
    // 1. 현재 사용자 확인 → 2. 선택 현장 권한 확인 → 3. 상태별 개수/경보 개수/분전반 목록 조회
    @Transactional(readOnly = true)
    public DashboardSummaryRes getSummary(DashboardSummaryReq req) {
        UserPrincipal actor = getCurrentUser();
        DashboardSummaryReq searchReq = normalizeReq(req);
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());
        Long siteId = searchReq.getSiteId();
        validateSiteAccess(actor, superAdmin, siteId);

        long totalPanelCount = dashboardMapper.countAccessiblePanels(actor.getUserId(), superAdmin, siteId);
        long normalPanelCount = countByStatus(actor, superAdmin, siteId, PanelStatus.NORMAL);
        long cautionPanelCount = countByStatus(actor, superAdmin, siteId, PanelStatus.CAUTION);
        long riskPanelCount = countByStatus(actor, superAdmin, siteId, PanelStatus.RISK);
        long offlinePanelCount = countByStatus(actor, superAdmin, siteId, PanelStatus.OFFLINE);
        long unconfirmedAlertCount = dashboardMapper.countUnconfirmedAlerts(actor.getUserId(), superAdmin, siteId);
        long unresolvedAlertCount = dashboardMapper.countUnresolvedAlerts(actor.getUserId(), superAdmin, siteId);
        List<DashboardPanelRes> panels = dashboardMapper.findDashboardPanels(actor.getUserId(), superAdmin, siteId);

        return new DashboardSummaryRes(
                totalPanelCount,
                normalPanelCount,
                cautionPanelCount,
                riskPanelCount,
                offlinePanelCount,
                unconfirmedAlertCount,
                unresolvedAlertCount,
                panels
        );
    }

    // null 요청도 전체 대시보드 조회로 처리
    private DashboardSummaryReq normalizeReq(DashboardSummaryReq req) {
        return req == null ? new DashboardSummaryReq() : req;
    }

    // 상태별 분전반 개수 조회
    private long countByStatus(UserPrincipal actor, boolean superAdmin, Long siteId, PanelStatus status) {
        return dashboardMapper.countAccessiblePanelsByStatus(actor.getUserId(), superAdmin, siteId, status.name());
    }

    // 선택 현장 접근 권한 확인
    private void validateSiteAccess(UserPrincipal actor, boolean superAdmin, Long siteId) {
        if (siteId == null) {
            return;
        }

        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        if (superAdmin) {
            return;
        }

        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
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
