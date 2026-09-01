package com.arcguard.firesafety.sensor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorFrame {

    private Long frameId;
    private Long panelId;
    private LocalDateTime receivedAt;
    private Integer mode;
    private BigDecimal voltV;
    private BigDecimal leakMa;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private Integer fireRaw;
    private Integer gasRaw;
    private String errorBits;
    private Boolean doorStatus;
    private BigDecimal totalCurrent;
    private Integer totalPower;
}
