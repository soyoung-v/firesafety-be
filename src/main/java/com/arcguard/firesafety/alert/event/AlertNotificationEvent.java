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
