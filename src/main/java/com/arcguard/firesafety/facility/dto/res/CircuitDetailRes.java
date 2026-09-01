package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Circuit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "회로 상세")
public class CircuitDetailRes {

    @Schema(description = "회로 ID", example = "1")
    private Long circuitId;
    @Schema(description = "소속 분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "회로 번호", example = "1")
    private Integer channelNo;
    @Schema(description = "부하 종류 메모", example = "조명")
    private String loadType;
    @Schema(description = "등록일시", example = "2026-07-23T10:00:00")
    private LocalDateTime createdAt;

    public static CircuitDetailRes from(Circuit circuit) {
        return new CircuitDetailRes(
                circuit.getCircuitId(),
                circuit.getPanelId(),
                circuit.getChannelNo(),
                circuit.getLoadType(),
                circuit.getCreatedAt()
        );
    }
}
