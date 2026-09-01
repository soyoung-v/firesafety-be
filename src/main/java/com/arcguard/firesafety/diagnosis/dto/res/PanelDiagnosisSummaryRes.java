package com.arcguard.firesafety.diagnosis.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PanelDiagnosisSummaryRes {

    private LocalDateTime latestDiagnosedAt;
    private Long totalCircuitCount;
    private Long diagnosedCircuitCount;
    private Long last24hTotalCount;
    private Long last24hNormalCount;
    private Long last24hArcCount;
    private List<PanelDiagnosisRecentRes> recentResults;
    private List<PanelDiagnosisRecentRes> recentArcResults;
    private List<PanelDiagnosisSampleStatusRes> sampleInsufficientCircuits;
}
