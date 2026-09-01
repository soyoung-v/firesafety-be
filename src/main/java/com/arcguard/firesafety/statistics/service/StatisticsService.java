package com.arcguard.firesafety.statistics.service;

import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.statistics.dto.req.StatisticsReq;
import com.arcguard.firesafety.statistics.dto.res.StatisticsAlertRes;
import com.arcguard.firesafety.statistics.dto.res.CircuitCountRow;
import com.arcguard.firesafety.statistics.dto.res.DailyResolutionRateRes;
import com.arcguard.firesafety.statistics.dto.res.DailyResolutionRow;
import com.arcguard.firesafety.statistics.dto.res.StatisticsCountRes;
import com.arcguard.firesafety.statistics.dto.res.InspectionCountRow;
import com.arcguard.firesafety.statistics.dto.res.StatisticsDiagnosisRes;
import com.arcguard.firesafety.statistics.dto.res.StatisticsGroupCount;
import com.arcguard.firesafety.statistics.dto.res.StatisticsInspectionRes;
import com.arcguard.firesafety.statistics.dto.res.StatisticsPanelRes;
import com.arcguard.firesafety.statistics.dto.res.StatisticsSummaryRes;
import com.arcguard.firesafety.statistics.mapper.StatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final int RECENT_INSPECTION_LIMIT = 5;

    private final StatisticsMapper statisticsMapper;

    // 통계 조회
    // 1. 현재 사용자 확인 → 2. 현장 권한 확인 → 3. 기간 계산 → 4. 경보/AI/분전반 통계 집계
    @Transactional(readOnly = true)
    public StatisticsSummaryRes getStatistics(StatisticsReq req) {
        UserPrincipal actor = getCurrentUser();
        StatisticsReq searchReq = normalizeReq(req);
        validateDateRange(searchReq);

        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());
        validateSiteAccess(actor, superAdmin, searchReq.getSiteId());

        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);

        return new StatisticsSummaryRes(
                searchReq.getFrom(),
                searchReq.getTo(),
                searchReq.getSiteId(),
                getAlertStatistics(actor, superAdmin, searchReq.getSiteId(), fromAt, toAt),
                getDiagnosisStatistics(actor, superAdmin, searchReq.getSiteId(), fromAt, toAt),
                getPanelStatistics(actor, superAdmin, searchReq.getSiteId()),
                getInspectionStatistics(actor, superAdmin, searchReq.getSiteId(), fromAt, toAt)
        );
    }

    // 경보 통계 집계
    private StatisticsAlertRes getAlertStatistics(UserPrincipal actor,
                                                  boolean superAdmin,
                                                  Long siteId,
                                                  LocalDateTime fromAt,
                                                  LocalDateTime toAt) {
        // 예방조치 이행률 추이는 주의(CAUTION) 알림만 대상 — 일자별 집계 1건으로 추이(dailyResolutionRates)와
        // 상단 지표(주의 알림 발생/조치완료/위험 전환 건수)를 함께 만든다(N+1 방지, 백엔드가 미리 계산).
        List<DailyResolutionRow> cautionRows = statisticsMapper.countDailyAlertResolutions(
                actor.getUserId(), superAdmin, siteId, fromAt, toAt);

        return new StatisticsAlertRes(
                statisticsMapper.countAlerts(actor.getUserId(), superAdmin, siteId, fromAt, toAt),
                toAlertStatusCounts(statisticsMapper.countAlertsByStatus(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                toAlertTypeCounts(statisticsMapper.countAlertsByType(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                toAlertSourceCounts(statisticsMapper.countAlertsBySource(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                statisticsMapper.countDailyAlerts(actor.getUserId(), superAdmin, siteId, fromAt, toAt),
                sumCount(cautionRows, DailyResolutionRow::getTotalCount),
                sumCount(cautionRows, DailyResolutionRow::getResolvedCount),
                sumCount(cautionRows, row -> row.getResolvedCount() - row.getPreventedCount()),
                toDailyResolutionRates(cautionRows)
        );
    }

    // 일자별 행 목록에서 특정 값을 합산 — 상단 지표는 일자별 집계를 다시 조회하지 않고 그대로 합산해서 구한다
    private long sumCount(List<DailyResolutionRow> rows, ToLongFunction<DailyResolutionRow> extractor) {
        return rows.stream().mapToLong(extractor).sum();
    }

    // 일자별 예방조치 이행률 계산 — 발생 건수가 0인 날은 비율을 null로 내려 "0%"와 "데이터 없음"을 구분한다
    private List<DailyResolutionRateRes> toDailyResolutionRates(List<DailyResolutionRow> rows) {
        return rows.stream()
                .map(row -> new DailyResolutionRateRes(
                        row.getDate(),
                        row.getTotalCount(),
                        row.getPreventedCount(),
                        row.getTotalCount() == 0
                                ? null
                                : Math.round(row.getPreventedCount() * 1000.0 / row.getTotalCount()) / 10.0
                ))
                .toList();
    }

    // AI 진단 통계 집계
    private StatisticsDiagnosisRes getDiagnosisStatistics(UserPrincipal actor,
                                                          boolean superAdmin,
                                                          Long siteId,
                                                          LocalDateTime fromAt,
                                                          LocalDateTime toAt) {
        CircuitCountRow circuitRow = statisticsMapper.countCircuitStats(actor.getUserId(), superAdmin, siteId, fromAt, toAt);
        return new StatisticsDiagnosisRes(
                statisticsMapper.countDiagnoses(actor.getUserId(), superAdmin, siteId, fromAt, toAt),
                toVerdictCounts(statisticsMapper.countDiagnosesByVerdict(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                circuitRow != null ? circuitRow.getTotalCircuitCount() : 0L,
                circuitRow != null ? circuitRow.getDiagnosedCircuitCount() : 0L
        );
    }

    // 현재 활성 분전반 상태 통계 집계
    private StatisticsPanelRes getPanelStatistics(UserPrincipal actor, boolean superAdmin, Long siteId) {
        return new StatisticsPanelRes(
                statisticsMapper.countActivePanels(actor.getUserId(), superAdmin, siteId),
                toPanelStatusCounts(statisticsMapper.countActivePanelsByStatus(actor.getUserId(), superAdmin, siteId))
        );
    }

    // 점검 현황 통계 집계 — 분전반별 반복 조회(N+1) 대신 집계 쿼리 1개 + 최근 이력 조회 1개로 계산한다
    private StatisticsInspectionRes getInspectionStatistics(UserPrincipal actor,
                                                             boolean superAdmin,
                                                             Long siteId,
                                                             LocalDateTime fromAt,
                                                             LocalDateTime toAt) {
        InspectionCountRow row = statisticsMapper.countInspectionStats(actor.getUserId(), superAdmin, siteId, fromAt, toAt);
        long totalPanelCount = row != null ? row.getTotalPanelCount() : 0L;
        long inspectedPanelCount = row != null ? row.getInspectedPanelCount() : 0L;
        long totalInspectionCount = row != null ? row.getTotalInspectionCount() : 0L;

        return new StatisticsInspectionRes(
                totalPanelCount,
                inspectedPanelCount,
                totalPanelCount - inspectedPanelCount,
                totalInspectionCount,
                statisticsMapper.findRecentInspections(actor.getUserId(), superAdmin, siteId, fromAt, toAt, RECENT_INSPECTION_LIMIT)
        );
    }

    // DB에 없는 enum 값도 0건으로 내려서 프론트 차트 범례가 흔들리지 않게 한다.
    private List<StatisticsCountRes> toAlertStatusCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(AlertStatus.values())
                .map(status -> new StatisticsCountRes(status.name(), status.getLabel(), countMap.getOrDefault(status.name(), 0L)))
                .toList();
    }

    // 경보 유형별 한글 라벨 추가
    private List<StatisticsCountRes> toAlertTypeCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(AlertType.values())
                .map(type -> new StatisticsCountRes(type.name(), type.getLabel(), countMap.getOrDefault(type.name(), 0L)))
                .toList();
    }

    // 경보 발생 소스별 한글 라벨 추가
    private List<StatisticsCountRes> toAlertSourceCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(AlertSource.values())
                .map(source -> new StatisticsCountRes(source.name(), source.getLabel(), countMap.getOrDefault(source.name(), 0L)))
                .toList();
    }

    // AI 판정별 한글 라벨 추가
    private List<StatisticsCountRes> toVerdictCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(Verdict.values())
                .map(verdict -> new StatisticsCountRes(verdict.name(), verdict.getLabel(), countMap.getOrDefault(verdict.name(), 0L)))
                .toList();
    }

    // 분전반 상태별 한글 라벨 추가
    private List<StatisticsCountRes> toPanelStatusCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(PanelStatus.values())
                .map(status -> new StatisticsCountRes(status.name(), status.getLabel(), countMap.getOrDefault(status.name(), 0L)))
                .toList();
    }

    // SQL 집계 row를 key-count Map으로 변환
    private Map<String, Long> toCountMap(List<StatisticsGroupCount> rows) {
        return rows.stream()
                .collect(Collectors.toMap(StatisticsGroupCount::getKey, StatisticsGroupCount::getCount));
    }

    // siteId가 있으면 현장 존재와 담당 배정을 확인
    private void validateSiteAccess(UserPrincipal actor, boolean superAdmin, Long siteId) {
        if (siteId == null) {
            return;
        }
        if (!statisticsMapper.existsSiteById(siteId)) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }
        if (superAdmin) {
            return;
        }
        if (!statisticsMapper.existsSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // null 요청도 전체 통계 조회로 처리
    private StatisticsReq normalizeReq(StatisticsReq req) {
        return req == null ? new StatisticsReq() : req;
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(StatisticsReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(StatisticsReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(StatisticsReq req) {
        return req.getTo() == null ? null : req.getTo().plusDays(1).atStartOfDay();
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
