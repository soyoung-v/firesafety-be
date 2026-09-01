package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.monitoring.event.MonitoringRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MonitoringRefreshEventListener {

    private final MonitoringRealtimeService monitoringRealtimeService;

    // 센서 수신 같은 관제 데이터 변경은 커밋 이후 화면 새로고침 이벤트로 알린다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(MonitoringRefreshEvent event) {
        monitoringRealtimeService.broadcastSiteRefresh(event.getSiteId(), event.getEventType());
    }
}
