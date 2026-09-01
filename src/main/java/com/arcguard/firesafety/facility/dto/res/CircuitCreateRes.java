package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Circuit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회로 등록 결과")
public class CircuitCreateRes {

    @Schema(description = "생성된 회로 ID", example = "1")
    private Long circuitId;
    @Schema(description = "소속 분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "회로 번호", example = "1")
    private Integer channelNo;
    @Schema(description = "부하 종류 메모", example = "조명")
    private String loadType;

    public static CircuitCreateRes from(Circuit circuit) {
        return new CircuitCreateRes(
                circuit.getCircuitId(),
                circuit.getPanelId(),
                circuit.getChannelNo(),
                circuit.getLoadType()
        );
    }
}
