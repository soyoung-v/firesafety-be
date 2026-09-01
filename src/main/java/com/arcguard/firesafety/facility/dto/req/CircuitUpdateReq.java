package com.arcguard.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회로 부하종류 수정 요청")
public class CircuitUpdateReq {

    @Schema(description = "부하 종류 메모(선택, 참고용). 50자 이하", example = "조명")
    private String loadType;
}
