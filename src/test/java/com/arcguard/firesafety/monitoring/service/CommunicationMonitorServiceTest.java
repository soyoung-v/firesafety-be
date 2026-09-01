package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.alert.service.SystemAlertService;
import com.arcguard.firesafety.monitoring.mapper.CommunicationMonitorMapper;
import com.arcguard.firesafety.monitoring.model.OfflinePanelTarget;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationMonitorServiceTest {

    @Mock
    private CommunicationMonitorMapper communicationMonitorMapper;

    @Mock
    private SystemAlertService systemAlertService;

    private CommunicationMonitorService communicationMonitorService;

    @BeforeEach
    void setUp() {
        LocalDateTime fixedDateTime = LocalDateTime.of(2026, 7, 23, 12, 0);
        Clock fixedClock = Clock.fixed(
                fixedDateTime.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        communicationMonitorService = new CommunicationMonitorService(
                communicationMonitorMapper,
                systemAlertService,
                fixedClock
        );
    }

    @Test
    @DisplayName("FR-01-04: 1분 이상 미수신 분전반을 OFFLINE으로 전환하고 경보를 생성한다")
    void detectCommunicationLost() {
        // given
        LocalDateTime thresholdAt = LocalDateTime.of(2026, 7, 23, 11, 59);
        when(communicationMonitorMapper.findOfflineTargets(thresholdAt)).thenReturn(List.of(target(10L)));
        when(communicationMonitorMapper.markPanelOffline(10L)).thenReturn(1);

        // when
        int changedCount = communicationMonitorService.detectCommunicationLost();

        // then
        assertThat(changedCount).isEqualTo(1);
        verify(communicationMonitorMapper).findOfflineTargets(thresholdAt);
        verify(systemAlertService).createCommunicationLostAlert(10L);
    }

    @Test
    @DisplayName("FR-01-04: 이미 OFFLINE 처리된 분전반은 경보를 새로 만들지 않는다")
    void skipAlreadyOfflinePanel() {
        // given
        LocalDateTime thresholdAt = LocalDateTime.of(2026, 7, 23, 11, 59);
        when(communicationMonitorMapper.findOfflineTargets(thresholdAt)).thenReturn(List.of(target(10L)));
        when(communicationMonitorMapper.markPanelOffline(10L)).thenReturn(0);

        // when
        int changedCount = communicationMonitorService.detectCommunicationLost();

        // then
        assertThat(changedCount).isZero();
        verify(systemAlertService, never()).createCommunicationLostAlert(10L);
    }

    private OfflinePanelTarget target(Long panelId) {
        OfflinePanelTarget target = new OfflinePanelTarget();
        target.setPanelId(panelId);
        target.setLastCommunicatedAt(LocalDateTime.of(2026, 7, 23, 11, 58));
        return target;
    }
}
