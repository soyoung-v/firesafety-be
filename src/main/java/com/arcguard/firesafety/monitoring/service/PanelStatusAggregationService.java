package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.alert.service.PanelCautionAlertService;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.monitoring.mapper.PanelStatusAggregationMapper;
import com.arcguard.firesafety.monitoring.model.CircuitStatusSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PanelStatusAggregationService {

    private static final Pattern HEX_8 = Pattern.compile("^[0-9A-Fa-f]{8}$");
    private static final long THRESHOLD_CAUTION_SECONDS = 30L;

    private final PanelStatusAggregationMapper panelStatusAggregationMapper;
    private final PanelCautionAlertService panelCautionAlertService;
    private final Clock clock;

    // 분전반 상태 집계
    // 1. 최신 하드웨어 판정 확인 → 2. 최신 AI/서버 수치 판정 확인 → 3. 최고 위험도 계산 → 4. panel.status 저장
    @Transactional
    public PanelStatus aggregatePanelStatus(Long panelId) {
        if (panelId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }

        String latestErrorBits = panelStatusAggregationMapper.findLatestErrorBitsByPanelId(panelId);
        Boolean latestDoorStatus = panelStatusAggregationMapper.findLatestDoorStatusByPanelId(panelId);
        List<CircuitStatusSnapshot> snapshots = panelStatusAggregationMapper.findCircuitStatusSnapshots(panelId);
        PanelStatus previousStatus = parsePanelStatus(panelStatusAggregationMapper.findCurrentPanelStatus(panelId));
        PanelStatus status = resolvePanelStatus(panelId, latestErrorBits, latestDoorStatus, snapshots);

        panelStatusAggregationMapper.updatePanelStatus(panelId, status.name());
        publishCautionTransition(panelId, previousStatus, status, latestDoorStatus);
        return status;
    }

    private void publishCautionTransition(Long panelId, PanelStatus previousStatus, PanelStatus status, Boolean latestDoorStatus) {
        if (status != PanelStatus.CAUTION || previousStatus == PanelStatus.CAUTION || previousStatus == PanelStatus.RISK) {
            return;
        }

        List<AlertType> types = new ArrayList<>();
        if (Boolean.TRUE.equals(latestDoorStatus)) {
            types.add(AlertType.DOOR_OPEN);
        }
        LocalDateTime thresholdAt = LocalDateTime.now(clock).minusSeconds(THRESHOLD_CAUTION_SECONDS);
        panelStatusAggregationMapper.findSustainedThresholdCautionTypes(panelId, thresholdAt).stream()
                .map(AlertType::valueOf)
                .forEach(types::add);

        panelCautionAlertService.createPanelCautionAlerts(panelId, types);
    }

    private PanelStatus parsePanelStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return PanelStatus.valueOf(status);
    }

    // 하드웨어 위험은 AI 결과보다 우선한다.
    private PanelStatus resolvePanelStatus(Long panelId, String latestErrorBits, Boolean latestDoorStatus, List<CircuitStatusSnapshot> snapshots) {
        if (hasDeviceRisk(latestErrorBits) || hasDeviceArc(snapshots)) {
            return PanelStatus.RISK;
        }
        if (Boolean.TRUE.equals(latestDoorStatus)) {
            return PanelStatus.CAUTION;
        }
        if (hasAiArc(snapshots)) {
            return PanelStatus.CAUTION;
        }
        if (hasSustainedThresholdCaution(panelId)) {
            return PanelStatus.CAUTION;
        }
        return PanelStatus.NORMAL;
    }

    // 하드웨어는 정상이어도 서버 주의 기준값이 30초 이상 지속되면 CAUTION
    private boolean hasSustainedThresholdCaution(Long panelId) {
        LocalDateTime thresholdAt = LocalDateTime.now(clock).minusSeconds(THRESHOLD_CAUTION_SECONDS);
        return panelStatusAggregationMapper.hasSustainedThresholdCaution(panelId, thresholdAt);
    }

    // aerror ARC/ERROR/ALARM bit가 하나라도 있으면 하드웨어 위험
    private boolean hasDeviceRisk(String errorBits) {
        if (!StringUtils.hasText(errorBits) || !HEX_8.matcher(errorBits).matches()) {
            return false;
        }
        int byte0 = parseByte(errorBits, 0);
        int byte1 = parseByte(errorBits, 1);
        int byte2 = parseByte(errorBits, 2);
        int byte3 = parseByte(errorBits, 3);

        return hasAnyBit(byte0, 0b0001_1111)
                || hasAnyBit(byte1, 0b0001_1111)
                || hasAnyBit(byte2, 0b0000_0111)
                || hasAnyBit(byte3, 0b0111_1111);
    }

    // 회로별 최신 하드웨어 ARC flag 확인
    private boolean hasDeviceArc(List<CircuitStatusSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return false;
        }
        return snapshots.stream().anyMatch(snapshot -> Boolean.TRUE.equals(snapshot.getDeviceArcFlag()));
    }

    // 하드웨어는 정상인데 AI만 ARC면 주의
    private boolean hasAiArc(List<CircuitStatusSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return false;
        }
        return snapshots.stream().anyMatch(snapshot -> snapshot.getLatestAiVerdict() == Verdict.ARC);
    }

    // aerror 8자리에서 1바이트를 16진수로 파싱
    private int parseByte(String errorBits, int byteIndex) {
        return Integer.parseInt(errorBits.substring(byteIndex * 2, byteIndex * 2 + 2), 16);
    }

    // 지정 mask에 해당하는 bit가 하나라도 켜져 있는지 확인
    private boolean hasAnyBit(int byteValue, int mask) {
        return (byteValue & mask) != 0;
    }
}
