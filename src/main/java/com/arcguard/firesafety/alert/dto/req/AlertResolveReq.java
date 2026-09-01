package com.arcguard.firesafety.alert.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "경보 조치완료 요청. body 없이도 호출 가능")
public class AlertResolveReq {

    @Schema(description = "조치 메모(선택). 경보 이력 엑셀의 '비고' 컬럼에 출력됨", example = "케이블 재접속")
    @Size(max = 500, message = "비고는 500자 이하로 입력해 주세요")
    private String resolutionNote;
}
