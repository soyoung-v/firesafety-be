package com.arcguard.firesafety.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "이메일 중복확인 결과")
public class EmailCheckRes {

    @Schema(description = "이미 사용 중인 이메일이면 true", example = "false")
    private boolean duplicate;
}
