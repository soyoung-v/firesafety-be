package com.arcguard.firesafety.sensor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorFrameCircuit {

    private Long readingId;
    private Long frameId;
    private Long circuitId;
    private BigDecimal currentA;
    private Integer arcCounter;
    private Boolean deviceArcFlag;
}
