package com.arcguard.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회로 등록 요청")
public class CircuitCreateReq {

    @Schema(description = "회로 번호(분전반 안에서 몇 번째 회로인지). 1~분전반 circuitCount 범위, 물리 최대 10", example = "1")
    private Integer channelNo;
    @Schema(description = "부하 종류 메모(선택, 참고용)", example = "조명")
    private String loadType;
}
