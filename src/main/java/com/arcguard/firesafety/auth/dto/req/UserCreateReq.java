package com.arcguard.firesafety.auth.dto.req;

import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "계정 등록 요청. SUPER_ADMIN/ADMIN은 ADMIN/GENERAL 등록 가능")
public class UserCreateReq {

    @Schema(description = "로그인 아이디로 쓸 이메일. 중복 불가", example = "new-user@example.com")
    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String email;

    @Schema(description = "초기 비밀번호. 공백 없이 영문+숫자 8자 이상", example = "password1234")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$",
            message = "비밀번호는 공백 없이 영문과 숫자를 포함하여 8자 이상 입력해 주세요")
    private String password;

    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @Schema(description = "전화번호(선택)", example = "01012345678")
    private String phone;

    @Schema(description = "등급. SUPER_ADMIN=플랫폼관리자, ADMIN=현장관리자, GENERAL=일반직원", example = "GENERAL")
    @NotNull(message = "역할은 필수입니다")
    private UserRole role;
}
