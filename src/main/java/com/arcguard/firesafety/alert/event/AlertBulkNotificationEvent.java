package com.arcguard.firesafety.alert.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

// 일괄 확인/조치완료 전용 — 대상이 몇 건이든 현장(site)당 WS 브로드캐스트를 딱 한 번만 보내기 위해
// 개별 AlertNotificationEvent와 분리했다. FCM은 대량 발송(사용자별 수십~수백 건 푸시) 방지를 위해
// 일부러 안 보낸다 — 리스너에서 realtime 갱신 신호만 쏜다.
@Getter
@AllArgsConstructor
public class AlertBulkNotificationEvent {

    private Set<Long> siteIds;
    private String eventType;
}
