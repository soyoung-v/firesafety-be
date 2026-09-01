package com.arcguard.firesafety.facility.model;

import com.arcguard.firesafety.diagnosis.model.Verdict;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// 회로 상세 조회용 원시값 (DB row 아님, 분전반 상세 API 조립용 조회 전용)
@Getter
@Setter
public class CircuitStatusRow {

    private Long circuitId;
    private Integer channelNo;
    private String loadType;
    private BigDecimal currentA;
    private Integer arcCounter;
    private Boolean deviceArcFlag;
    private Verdict latestAiVerdict;
}
