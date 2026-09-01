package com.arcguard.firesafety.facility.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Panel {

    private Long panelId;
    private Long siteId;
    private String name;
    private String deviceSerial;
    private String mNo;
    private LocalDate installedAt;
    private PanelStatus status;
    private Boolean isOnline;
    private LocalDateTime lastCommunicatedAt;
    private Integer circuitCount;
    private BigDecimal leakMaThreshold;
    private BigDecimal tempThreshold;
    private BigDecimal humidityThreshold;
    private BigDecimal overcurrentThreshold;
    private Integer gasThreshold;
    private Integer fireThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
