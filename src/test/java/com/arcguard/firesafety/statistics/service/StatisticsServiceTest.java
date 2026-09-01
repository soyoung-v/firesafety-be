package com.arcguard.firesafety.statistics.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.statistics.dto.req.StatisticsReq;
import com.arcguard.firesafety.statistics.dto.res.CircuitCountRow;
import com.arcguard.firesafety.statistics.dto.res.DailyAlertCountRes;
import com.arcguard.firesafety.statistics.dto.res.DailyResolutionRow;
import com.arcguard.firesafety.statistics.dto.res.InspectionCountRow;
import com.arcguard.firesafety.statistics.dto.res.StatisticsGroupCount;
import com.arcguard.firesafety.statistics.dto.res.StatisticsSummaryRes;
import com.arcguard.firesafety.statistics.mapper.StatisticsMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private StatisticsMapper statisticsMapper;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(statisticsMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-024: SUPER_ADMIN은 기간/현장 조건으로 통계를 조회할 수 있다")
    void superAdminCanGetStatistics() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        StatisticsReq req = new StatisticsReq();
        req.setSiteId(3L);
        req.setFrom(LocalDate.of(2026, 7, 1));
        req.setTo(LocalDate.of(2026, 7, 23));

        LocalDateTime fromAt = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime toAt = LocalDateTime.of(2026, 7, 24, 0, 0);
        when(statisticsMapper.existsSiteById(3L)).thenReturn(true);
        when(statisticsMapper.countAlerts(1L, true, 3L, fromAt, toAt)).thenReturn(5L);
        when(statisticsMapper.countAlertsByStatus(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("UNCONFIRMED", 2), group("RESOLVED", 3)));
        when(statisticsMapper.countAlertsByType(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("ARC", 4)));
        when(statisticsMapper.countAlertsBySource(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("DEVICE", 5)));
        when(statisticsMapper.countDailyAlerts(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(daily(LocalDate.of(2026, 7, 23), 5)));
        when(statisticsMapper.countDiagnoses(1L, true, 3L, fromAt, toAt)).thenReturn(2L);
        when(statisticsMapper.countDiagnosesByVerdict(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("ARC", 1)));
        when(statisticsMapper.countCircuitStats(1L, true, 3L, fromAt, toAt)).thenReturn(circuitCountRow(40, 18));
        when(statisticsMapper.countActivePanels(1L, true, 3L)).thenReturn(7L);
        when(statisticsMapper.countActivePanelsByStatus(1L, true, 3L))
                .thenReturn(List.of(group("RISK", 1)));
        when(statisticsMapper.countInspectionStats(1L, true, 3L, fromAt, toAt)).thenReturn(inspectionCountRow(7, 4, 9));
        when(statisticsMapper.findRecentInspections(1L, true, 3L, fromAt, toAt, 5)).thenReturn(List.of());

        // when
        StatisticsSummaryRes result = statisticsService.getStatistics(req);

        // then
        assertThat(result.getAlerts().getTotalCount()).isEqualTo(5L);
        assertThat(result.getAlerts().getStatusCounts())
                .anySatisfy(count -> {
                    assertThat(count.getKey()).isEqualTo("UNCONFIRMED");
                    assertThat(count.getCount()).isEqualTo(2L);
                })
                .anySatisfy(count -> {
                    assertThat(count.getKey()).isEqualTo("CONFIRMED");
                    assertThat(count.getCount()).isZero();
                });
        assertThat(result.getDiagnoses().getTotalCount()).isEqualTo(2L);
        assertThat(result.getDiagnoses().getTotalCircuitCount()).isEqualTo(40L);
        assertThat(result.getDiagnoses().getDiagnosedCircuitCount()).isEqualTo(18L);
        assertThat(result.getPanels().getTotalCount()).isEqualTo(7L);
        assertThat(result.getInspections().getTotalPanelCount()).isEqualTo(7L);
        assertThat(result.getInspections().getInspectedPanelCount()).isEqualTo(4L);
        assertThat(result.getInspections().getUninspectedPanelCount()).isEqualTo(3L);
        assertThat(result.getInspections().getTotalInspectionCount()).isEqualTo(9L);
        verify(statisticsMapper).countAlerts(1L, true, 3L, fromAt, toAt);
    }

    @Test
    @DisplayName("API-024: ADMIN은 배정되지 않은 현장 통계를 조회할 수 없다")
    void adminCannotGetUnassignedSiteStatistics() {
        // given
        loginAs(2L, UserRole.ADMIN);
        StatisticsReq req = new StatisticsReq();
        req.setSiteId(3L);
        when(statisticsMapper.existsSiteById(3L)).thenReturn(true);
        when(statisticsMapper.existsSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> statisticsService.getStatistics(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("API-024: 시작일이 종료일보다 늦으면 400을 반환한다")
    void invalidDateRangeFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        StatisticsReq req = new StatisticsReq();
        req.setFrom(LocalDate.of(2026, 7, 24));
        req.setTo(LocalDate.of(2026, 7, 23));

        // when & then
        assertThatThrownBy(() -> statisticsService.getStatistics(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    @DisplayName("API-024: 일자별 예방조치 이행률은 주의 알림 발생 건수 대비 예방성공(조치완료+위험미전환) 비율(%)로 계산되고, 발생 건수 0인 날은 null이다")
    void dailyResolutionRateCalculated() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        StatisticsReq req = new StatisticsReq();
        LocalDateTime fromAt = null;
        LocalDateTime toAt = null;
        when(statisticsMapper.countDailyAlertResolutions(1L, true, null, fromAt, toAt))
                .thenReturn(List.of(
                        resolutionRow(LocalDate.of(2026, 7, 23), 5, 4, 4),
                        resolutionRow(LocalDate.of(2026, 7, 24), 0, 0, 0)
                ));
        when(statisticsMapper.countCircuitStats(1L, true, null, fromAt, toAt)).thenReturn(circuitCountRow(0, 0));
        when(statisticsMapper.countInspectionStats(1L, true, null, fromAt, toAt)).thenReturn(inspectionCountRow(0, 0, 0));
        when(statisticsMapper.findRecentInspections(1L, true, null, fromAt, toAt, 5)).thenReturn(List.of());

        // when
        StatisticsSummaryRes result = statisticsService.getStatistics(req);

        // then
        List<com.arcguard.firesafety.statistics.dto.res.DailyResolutionRateRes> rates = result.getAlerts().getDailyResolutionRates();
        assertThat(rates.get(0).getRate()).isEqualTo(80.0);
        assertThat(rates.get(1).getRate()).isNull();
        assertThat(result.getAlerts().getCautionAlertCount()).isEqualTo(5L);
        assertThat(result.getAlerts().getCautionResolvedCount()).isEqualTo(4L);
        assertThat(result.getAlerts().getCautionEscalatedCount()).isZero();
    }

    @Test
    @DisplayName("API-024: 조치완료 후 24시간 내 위험 전환된 건수는 위험 전환 건수로 집계되고 예방성공에서 제외된다")
    void escalatedCautionExcludedFromPreventionRate() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        StatisticsReq req = new StatisticsReq();
        LocalDateTime fromAt = null;
        LocalDateTime toAt = null;
        // 발생 5건, 조치완료 4건, 그중 1건은 조치완료 후 24시간 내 위험 전환 → 예방성공은 3건
        when(statisticsMapper.countDailyAlertResolutions(1L, true, null, fromAt, toAt))
                .thenReturn(List.of(resolutionRow(LocalDate.of(2026, 7, 23), 5, 4, 3)));
        when(statisticsMapper.countCircuitStats(1L, true, null, fromAt, toAt)).thenReturn(circuitCountRow(0, 0));
        when(statisticsMapper.countInspectionStats(1L, true, null, fromAt, toAt)).thenReturn(inspectionCountRow(0, 0, 0));
        when(statisticsMapper.findRecentInspections(1L, true, null, fromAt, toAt, 5)).thenReturn(List.of());

        // when
        StatisticsSummaryRes result = statisticsService.getStatistics(req);

        // then
        List<com.arcguard.firesafety.statistics.dto.res.DailyResolutionRateRes> rates = result.getAlerts().getDailyResolutionRates();
        assertThat(rates.get(0).getRate()).isEqualTo(60.0);
        assertThat(result.getAlerts().getCautionAlertCount()).isEqualTo(5L);
        assertThat(result.getAlerts().getCautionResolvedCount()).isEqualTo(4L);
        assertThat(result.getAlerts().getCautionEscalatedCount()).isEqualTo(1L);
    }

    private DailyResolutionRow resolutionRow(LocalDate date, long totalCount, long resolvedCount, long preventedCount) {
        DailyResolutionRow row = new DailyResolutionRow();
        row.setDate(date);
        row.setTotalCount(totalCount);
        row.setResolvedCount(resolvedCount);
        row.setPreventedCount(preventedCount);
        return row;
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private StatisticsGroupCount group(String key, long count) {
        StatisticsGroupCount group = new StatisticsGroupCount();
        group.setKey(key);
        group.setCount(count);
        return group;
    }

    private DailyAlertCountRes daily(LocalDate date, long count) {
        DailyAlertCountRes daily = new DailyAlertCountRes();
        daily.setDate(date);
        daily.setCount(count);
        return daily;
    }

    private InspectionCountRow inspectionCountRow(long totalPanelCount, long inspectedPanelCount, long totalInspectionCount) {
        InspectionCountRow row = new InspectionCountRow();
        row.setTotalPanelCount(totalPanelCount);
        row.setInspectedPanelCount(inspectedPanelCount);
        row.setTotalInspectionCount(totalInspectionCount);
        return row;
    }

    private CircuitCountRow circuitCountRow(long totalCircuitCount, long diagnosedCircuitCount) {
        CircuitCountRow row = new CircuitCountRow();
        row.setTotalCircuitCount(totalCircuitCount);
        row.setDiagnosedCircuitCount(diagnosedCircuitCount);
        return row;
    }
}
