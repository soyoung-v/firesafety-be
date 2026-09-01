package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Circuit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회로 목록 항목")
public class CircuitListRes {

    @Schema(description = "회로 ID", example = "1")
    private Long circuitId;
    @Schema(description = "소속 분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "회로 번호", example = "1")
    private Integer channelNo;
    @Schema(description = "부하 종류 메모", example = "조명")
    private String loadType;

    public static CircuitListRes from(Circuit circuit) {
        return new CircuitListRes(
                circuit.getCircuitId(),
                circuit.getPanelId(),
                circuit.getChannelNo(),
                circuit.getLoadType()
        );
    }
}
