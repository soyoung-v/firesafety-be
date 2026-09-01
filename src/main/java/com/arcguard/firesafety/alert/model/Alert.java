package com.arcguard.firesafety.alert.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    private Long alertId;
    private Long circuitId;
    private Long panelId;
    private AlertSource source;
    private AlertType type;
    private AlertSeverity severity;
    private Long resultId;
    private AlertStatus status;
    private LocalDateTime triggeredAt;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private String resolutionNote;
}
