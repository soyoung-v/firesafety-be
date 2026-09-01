package com.arcguard.firesafety.diagnosis.dto.res;

import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PanelDiagnosisRecentRes {

    private Long resultId;
    private Long circuitId;
    private Integer channelNo;
    private Verdict verdict;
    private Float confidence;
    private Integer nSamples;
    private String warning;
    private DiagnosisTriggerType triggerType;
    private LocalDateTime diagnosedAt;
}
