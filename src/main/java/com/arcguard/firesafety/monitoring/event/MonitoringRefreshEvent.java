package com.arcguard.firesafety.monitoring.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonitoringRefreshEvent {

    private Long siteId;
    private String eventType;
}
