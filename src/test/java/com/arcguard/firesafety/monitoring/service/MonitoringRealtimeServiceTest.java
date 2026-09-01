package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.monitoring.dto.res.MonitoringRealtimeEventRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitoringRealtimeServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MonitoringRealtimeService monitoringRealtimeService;

    @BeforeEach
    void setUp() {
        monitoringRealtimeService = new MonitoringRealtimeService(messagingTemplate);
    }

    @Test
    @DisplayName("MON-002: 담당 현장 관제 화면에 새로고침 이벤트를 브로드캐스트한다")
    void broadcastSiteRefreshEvent() {
        // when
        monitoringRealtimeService.broadcastSiteRefresh(3L, "ALERT_CREATED");

        // then
        ArgumentCaptor<MonitoringRealtimeEventRes> eventCaptor = ArgumentCaptor.forClass(MonitoringRealtimeEventRes.class);
        verify(messagingTemplate).convertAndSend(org.mockito.Mockito.eq("/topic/sites/3/monitoring"), eventCaptor.capture());

        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("ALERT_CREATED");
        assertThat(eventCaptor.getValue().getOccurredAt()).isNotNull();
    }
}
