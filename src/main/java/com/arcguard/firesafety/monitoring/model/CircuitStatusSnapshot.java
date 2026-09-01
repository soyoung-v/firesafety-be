package com.arcguard.firesafety.monitoring.model;

import com.arcguard.firesafety.diagnosis.model.Verdict;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircuitStatusSnapshot {

    private Long circuitId;
    private Integer channelNo;
    private Boolean deviceArcFlag;
    private Verdict latestAiVerdict;
}
