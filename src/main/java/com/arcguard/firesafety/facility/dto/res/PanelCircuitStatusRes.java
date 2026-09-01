package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "분전반 상세의 회로 상태 항목")
public class PanelCircuitStatusRes {

    @Schema(description = "회로 ID", example = "1")
    private Long circuitId;
    @Schema(description = "회로 번호", example = "1")
    private Integer channelNo;
    @Schema(description = "부하 종류 메모", example = "조명")
    private String loadType;
    @Schema(description = "최신 전류값(A)", example = "8.8")
    private BigDecimal currentA;
    @Schema(description = "아크 카운터 누적값. 0=정상, 증가 시 아크 의심", example = "0")
    private Integer arcCounter;
    @Schema(description = "회로 상태(NORMAL/CAUTION/RISK/OFFLINE). 분전반이 OFFLINE이면 소속 회로도 전부 OFFLINE", example = "NORMAL")
    private PanelStatus status;
}
