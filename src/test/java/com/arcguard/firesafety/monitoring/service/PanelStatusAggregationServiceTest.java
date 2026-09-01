package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.alert.service.PanelCautionAlertService;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.monitoring.mapper.PanelStatusAggregationMapper;
import com.arcguard.firesafety.monitoring.model.CircuitStatusSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanelStatusAggregationServiceTest {

    @Mock
    private PanelStatusAggregationMapper panelStatusAggregationMapper;

    @Mock
    private PanelCautionAlertService panelCautionAlertService;

    private PanelStatusAggregationService panelStatusAggregationService;
    private LocalDateTime thresholdAt;

    @BeforeEach
    void setUp() {
        LocalDateTime fixedNow = LocalDateTime.of(2026, 7, 23, 12, 0);
        Clock fixedClock = Clock.fixed(
                fixedNow.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        thresholdAt = fixedNow.minusSeconds(30);
        panelStatusAggregationService =
                new PanelStatusAggregationService(panelStatusAggregationMapper, panelCautionAlertService, fixedClock);
    }

    @Test
    @DisplayName("FR-03-03: 하드웨어 위험 bit가 있으면 분전반 상태를 RISK로 집계한다")
    void deviceRiskAggregatesPanelAsRisk() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("18000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.NORMAL)));

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.RISK);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "RISK");
    }

    @Test
    @DisplayName("FR-03-03: 하드웨어 정상이고 AI가 ARC이면 분전반 상태를 CAUTION으로 집계한다")
    void aiArcAggregatesPanelAsCaution() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("00000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.ARC)));

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.CAUTION);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "CAUTION");
    }

    @Test
    @DisplayName("FR-03-03: 도어가 열려 있으면 하드웨어 알람 bit가 없어도 분전반 상태를 CAUTION으로 집계한다")
    void openDoorAggregatesPanelAsCaution() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("00000000");
        when(panelStatusAggregationMapper.findLatestDoorStatusByPanelId(10L)).thenReturn(true);
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.NORMAL)));

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.CAUTION);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "CAUTION");
    }

    @Test
    @DisplayName("FR-03-03: 하드웨어와 AI가 모두 정상이면 분전반 상태를 NORMAL로 집계한다")
    void normalSignalsAggregatePanelAsNormal() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("00000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.NORMAL)));
        when(panelStatusAggregationMapper.hasSustainedThresholdCaution(10L, thresholdAt)).thenReturn(false);

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.NORMAL);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "NORMAL");
    }

    @Test
    @DisplayName("FR-03-03: 서버 주의 기준값이 30초 이상 지속되면 분전반 상태를 CAUTION으로 집계한다")
    void sustainedThresholdAggregatesPanelAsCaution() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("00000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.NORMAL)));
        when(panelStatusAggregationMapper.hasSustainedThresholdCaution(10L, thresholdAt)).thenReturn(true);

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.CAUTION);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "CAUTION");
    }

    private CircuitStatusSnapshot snapshot(boolean deviceArcFlag, Verdict verdict) {
        CircuitStatusSnapshot snapshot = new CircuitStatusSnapshot();
        snapshot.setCircuitId(1L);
        snapshot.setChannelNo(1);
        snapshot.setDeviceArcFlag(deviceArcFlag);
        snapshot.setLatestAiVerdict(verdict);
        return snapshot;
    }
}
