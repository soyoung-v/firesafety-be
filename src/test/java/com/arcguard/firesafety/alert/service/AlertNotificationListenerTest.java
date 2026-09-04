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

import static org.mockito.Mockito.never;
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
    @DisplayName("FR-04-02/MON-002: 신규 경보(ALERT_CREATED)는 WebSocket과 FCM 둘 다로 전달한다")
    void handleAlertCreatedEvent() {
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
                AlertNotificationEvent.EVENT_ALERT_CREATED
        );

        // when
        alertNotificationListener.handle(event);

        // then
        verify(monitoringRealtimeService).broadcastSiteRefresh(3L, AlertNotificationEvent.EVENT_ALERT_CREATED);
        verify(fcmPushService).sendAlert(event);
    }

    @Test
    @DisplayName("상태 변경(확인/조치완료) 이벤트는 WebSocket만 보내고 FCM은 재발송하지 않는다")
    void handleStatusChangedEventDoesNotSendFcm() {
        // given - AlertService.confirmAlert/resolveAlert가 publishStatusChanged로 발행하는 것과 동일한 형태
        AlertNotificationEvent event = new AlertNotificationEvent(
                100L,
                3L,
                10L,
                20L,
                AlertSource.DEVICE,
                AlertType.ARC,
                AlertSeverity.RISK,
                AlertStatus.CONFIRMED,
                "ALERT_STATUS_CHANGED"
        );

        // when
        alertNotificationListener.handle(event);

        // then
        verify(monitoringRealtimeService).broadcastSiteRefresh(3L, "ALERT_STATUS_CHANGED");
        verify(fcmPushService, never()).sendAlert(event);
    }

    @Test
    @DisplayName("조치완료(RESOLVED) 상태 변경도 FCM을 재발송하지 않는다")
    void handleResolvedStatusChangedEventDoesNotSendFcm() {
        // given
        AlertNotificationEvent event = new AlertNotificationEvent(
                101L,
                3L,
                10L,
                20L,
                AlertSource.DEVICE,
                AlertType.ARC,
                AlertSeverity.RISK,
                AlertStatus.RESOLVED,
                "ALERT_STATUS_CHANGED"
        );

        // when
        alertNotificationListener.handle(event);

        // then
        verify(monitoringRealtimeService).broadcastSiteRefresh(3L, "ALERT_STATUS_CHANGED");
        verify(fcmPushService, never()).sendAlert(event);
    }
}
