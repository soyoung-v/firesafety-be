package com.arcguard.firesafety.alert.event;

import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertNotificationEvent {

    // 신규 경보 생성 이벤트 - FCM은 이 eventType일 때만 보낸다(AlertNotificationListener 참고).
    // 상태 변경(확인/조치완료)은 AlertNotificationPublisher.publishStatusChanged가 별도 문자열을 쓴다.
    public static final String EVENT_ALERT_CREATED = "ALERT_CREATED";

    private Long alertId;
    private Long siteId;
    private Long panelId;
    private Long circuitId;
    private AlertSource source;
    private AlertType type;
    private AlertSeverity severity;
    private AlertStatus status;
    private String eventType;
}
