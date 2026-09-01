package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.facility.model.Site;
import com.arcguard.firesafety.monitoring.dto.req.DashboardSummaryReq;
import com.arcguard.firesafety.monitoring.dto.res.DashboardPanelRes;
import com.arcguard.firesafety.monitoring.dto.res.DashboardSummaryRes;
import com.arcguard.firesafety.monitoring.mapper.DashboardMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private SiteMapper siteMapper;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(dashboardMapper, siteMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-018: SUPER_ADMIN은 전체 현장 기준 대시보드 요약을 조회한다")
    void superAdminCanGetDashboardSummary() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(dashboardMapper.countAccessiblePanels(1L, true, null)).thenReturn(4L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, null, "NORMAL")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, null, "CAUTION")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, null, "RISK")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, null, "OFFLINE")).thenReturn(1L);
        when(dashboardMapper.countUnconfirmedAlerts(1L, true, null)).thenReturn(3L);
        when(dashboardMapper.countUnresolvedAlerts(1L, true, null)).thenReturn(5L);
        when(dashboardMapper.findDashboardPanels(1L, true, null)).thenReturn(List.of(panel(10L, PanelStatus.OFFLINE)));

        // when
        DashboardSummaryRes result = dashboardService.getSummary(new DashboardSummaryReq());

        // then
        assertThat(result.getTotalPanelCount()).isEqualTo(4L);
        assertThat(result.getOfflinePanelCount()).isEqualTo(1L);
        assertThat(result.getUnconfirmedAlertCount()).isEqualTo(3L);
        assertThat(result.getUnresolvedAlertCount()).isEqualTo(5L);
        assertThat(result.getPanels().get(0).getStatus()).isEqualTo(PanelStatus.OFFLINE);
    }

    @Test
    @DisplayName("API-018: ADMIN은 담당 현장 기준으로 대시보드 요약을 조회한다")
    void adminCanGetAssignedSiteDashboardSummary() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(dashboardMapper.countAccessiblePanels(2L, false, null)).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, null, "NORMAL")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, null, "CAUTION")).thenReturn(0L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, null, "RISK")).thenReturn(0L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, null, "OFFLINE")).thenReturn(0L);
        when(dashboardMapper.countUnconfirmedAlerts(2L, false, null)).thenReturn(0L);
        when(dashboardMapper.countUnresolvedAlerts(2L, false, null)).thenReturn(0L);
        when(dashboardMapper.findDashboardPanels(2L, false, null)).thenReturn(List.of(panel(20L, PanelStatus.NORMAL)));

        // when
        DashboardSummaryRes result = dashboardService.getSummary(new DashboardSummaryReq());

        // then
        assertThat(result.getTotalPanelCount()).isEqualTo(1L);
        verify(dashboardMapper).findDashboardPanels(2L, false, null);
    }

    @Test
    @DisplayName("API-018: SUPER_ADMIN은 선택 현장 기준으로 대시보드 요약을 조회한다")
    void superAdminCanFilterDashboardBySite() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        DashboardSummaryReq req = new DashboardSummaryReq();
        req.setSiteId(3L);

        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(dashboardMapper.countAccessiblePanels(1L, true, 3L)).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, 3L, "NORMAL")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, 3L, "CAUTION")).thenReturn(0L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, 3L, "RISK")).thenReturn(0L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, 3L, "OFFLINE")).thenReturn(0L);
        when(dashboardMapper.countUnconfirmedAlerts(1L, true, 3L)).thenReturn(0L);
        when(dashboardMapper.countUnresolvedAlerts(1L, true, 3L)).thenReturn(0L);
        when(dashboardMapper.findDashboardPanels(1L, true, 3L)).thenReturn(List.of(panel(20L, PanelStatus.NORMAL)));

        // when
        DashboardSummaryRes result = dashboardService.getSummary(req);

        // then
        assertThat(result.getTotalPanelCount()).isEqualTo(1L);
        verify(dashboardMapper).findDashboardPanels(1L, true, 3L);
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private DashboardPanelRes panel(Long panelId, PanelStatus status) {
        DashboardPanelRes panel = new DashboardPanelRes();
        panel.setPanelId(panelId);
        panel.setSiteId(1L);
        panel.setSiteName("아크타워 본사");
        panel.setName("분전반A");
        panel.setStatus(status);
        panel.setLastCommunicatedAt(LocalDateTime.of(2026, 7, 23, 11, 0));
        panel.setUnconfirmedAlertCount(1L);
        return panel;
    }

    private Site site() {
        Site site = new Site();
        site.setSiteId(3L);
        return site;
    }
}
