package com.arcguard.firesafety.diagnosis.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiPredictionCircuitTarget {

    private Long circuitId;
    private Integer channelNo;
    private Long latestFrameId;
}
