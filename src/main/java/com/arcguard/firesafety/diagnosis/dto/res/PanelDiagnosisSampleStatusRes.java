package com.arcguard.firesafety.diagnosis.dto.res;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PanelDiagnosisSampleStatusRes {

    private Long circuitId;
    private Integer channelNo;
    private Integer sampleCount;
    private Integer requiredSampleCount;
}
