package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.monitoring.dto.res.MonitoringRealtimeEventRes;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MonitoringRealtimeService {

    private static final String MONITORING_TOPIC = "/topic/monitoring";
    private static final String SITE_MONITORING_TOPIC_PREFIX = "/topic/sites/";
    private static final String SITE_MONITORING_TOPIC_SUFFIX = "/monitoring";

    private final SimpMessagingTemplate messagingTemplate;

    // 담당 현장 관제 화면에 데이터 변경 알림 전송
    // 상세 데이터는 보호된 REST API로 다시 조회해서 권한 범위를 지킨다.
    public void broadcastSiteRefresh(Long siteId, String eventType) {
        MonitoringRealtimeEventRes event = new MonitoringRealtimeEventRes(eventType, LocalDateTime.now());
        messagingTemplate.convertAndSend(MONITORING_TOPIC, event);

        if (siteId != null) {
            messagingTemplate.convertAndSend(buildSiteTopic(siteId), event);
        }
    }

    // 현장별 STOMP topic 생성
    private String buildSiteTopic(Long siteId) {
        return SITE_MONITORING_TOPIC_PREFIX + siteId + SITE_MONITORING_TOPIC_SUFFIX;
    }
}
