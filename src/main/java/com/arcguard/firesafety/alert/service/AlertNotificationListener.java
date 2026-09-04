package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.event.AlertBulkNotificationEvent;
import com.arcguard.firesafety.alert.event.AlertNotificationEvent;
import com.arcguard.firesafety.monitoring.service.MonitoringRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotificationListener {

    private final MonitoringRealtimeService monitoringRealtimeService;
    private final FcmPushService fcmPushService;

    // 경보 DB 반영이 끝난 뒤 WebSocket/FCM 알림 처리
    // FCM은 신규 생성(ALERT_CREATED)일 때만 보낸다 - 확인/조치완료(publishStatusChanged가 보내는 다른
    // eventType)는 화면 갱신용 WebSocket 브로드캐스트만 필요하고, 사용자에게 다시 푸시를 보내면 안 된다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(AlertNotificationEvent event) {
        monitoringRealtimeService.broadcastSiteRefresh(event.getSiteId(), event.getEventType());
        if (AlertNotificationEvent.EVENT_ALERT_CREATED.equals(event.getEventType())) {
            fcmPushService.sendAlert(event);
        }
    }

    // 일괄 확인/조치완료 — 건마다 브로드캐스트하면 클라이언트 재조회가 폭주하므로 현장당 한 번만 보낸다.
    // FCM은 일부러 안 보낸다(대량 처리 시 사용자별 수십~수백 건 푸시가 쏟아지는 걸 방지).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleBulk(AlertBulkNotificationEvent event) {
        event.getSiteIds().forEach(siteId -> monitoringRealtimeService.broadcastSiteRefresh(siteId, event.getEventType()));
    }
}
