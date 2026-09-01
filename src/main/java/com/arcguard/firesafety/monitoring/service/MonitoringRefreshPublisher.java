package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.monitoring.event.MonitoringRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoringRefreshPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    // 관제 화면 새로고침 이벤트 발행
    public void publish(Long siteId, String eventType) {
        applicationEventPublisher.publishEvent(new MonitoringRefreshEvent(siteId, eventType));
    }
}
