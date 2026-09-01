package com.arcguard.firesafety.monitoring.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OfflinePanelTarget {

    private Long panelId;
    private LocalDateTime lastCommunicatedAt;
}
