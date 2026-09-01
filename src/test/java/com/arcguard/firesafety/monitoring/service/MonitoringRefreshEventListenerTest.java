package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.monitoring.event.MonitoringRefreshEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitoringRefreshEventListenerTest {

    @Mock
    private MonitoringRealtimeService monitoringRealtimeService;

    private MonitoringRefreshEventListener monitoringRefreshEventListener;

    @BeforeEach
    void setUp() {
        monitoringRefreshEventListener = new MonitoringRefreshEventListener(monitoringRealtimeService);
    }

    @Test
    @DisplayName("MON-002: 센서 수신 이벤트를 담당 현장 WebSocket으로 전달한다")
    void handleMonitoringRefreshEvent() {
        // given
        MonitoringRefreshEvent event = new MonitoringRefreshEvent(3L, "SENSOR_FRAME_RECEIVED");

        // when
        monitoringRefreshEventListener.handle(event);

        // then
        verify(monitoringRealtimeService).broadcastSiteRefresh(3L, "SENSOR_FRAME_RECEIVED");
    }
}
