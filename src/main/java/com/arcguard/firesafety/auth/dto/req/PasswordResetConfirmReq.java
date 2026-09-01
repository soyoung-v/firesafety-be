package com.arcguard.firesafety.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 확정 요청")
public class PasswordResetConfirmReq {

    @Schema(description = "이메일로 받은 재설정 링크의 원본 토큰값", example = "abcdef123456")
    @NotBlank(message = "토큰은 필수입니다")
    private String token;

    @Schema(description = "새 비밀번호. 공백 없이 영문+숫자 8자 이상", example = "newPassword1234")
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$",
            message = "비밀번호는 공백 없이 영문과 숫자를 포함하여 8자 이상 입력해 주세요")
    private String newPassword;
}
