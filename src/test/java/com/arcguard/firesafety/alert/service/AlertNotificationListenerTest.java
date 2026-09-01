package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.event.AlertNotificationEvent;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.monitoring.service.MonitoringRealtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertNotificationListenerTest {

    @Mock
    private MonitoringRealtimeService monitoringRealtimeService;

    @Mock
    private FcmPushService fcmPushService;

    private AlertNotificationListener alertNotificationListener;

    @BeforeEach
    void setUp() {
        alertNotificationListener = new AlertNotificationListener(monitoringRealtimeService, fcmPushService);
    }

    @Test
    @DisplayName("FR-04-02/MON-002: 경보 이벤트를 WebSocket과 FCM으로 전달한다")
    void handleAlertNotificationEvent() {
        // given
        AlertNotificationEvent event = new AlertNotificationEvent(
                100L,
                3L,
                10L,
                20L,
                AlertSource.DEVICE,
                AlertType.ARC,
                AlertSeverity.RISK,
                AlertStatus.UNCONFIRMED,
                "ALERT_CREATED"
        );

        // when
        alertNotificationListener.handle(event);

        // then
        verify(monitoringRealtimeService).broadcastSiteRefresh(3L, "ALERT_CREATED");
        verify(fcmPushService).sendAlert(event);
    }
}
