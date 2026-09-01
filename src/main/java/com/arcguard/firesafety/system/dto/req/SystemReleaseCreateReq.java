package com.arcguard.firesafety.system.dto.req;

import com.arcguard.firesafety.system.model.SystemReleaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "업데이트 이력 등록 요청. SUPER_ADMIN 전용")
public class SystemReleaseCreateReq {

    @NotNull
    @Schema(description = "구분(SOFTWARE/MODEL)", example = "SOFTWARE")
    private SystemReleaseType type;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "버전. SOFTWARE는 유의적 버전(Major.Minor.Patch) 형식만 허용", example = "1.2.0")
    private String version;

    @Size(max = 255)
    @Schema(description = "변경 내용(선택)", example = "아크 감지 임계값 튜닝, 통계 화면 추가")
    private String description;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "업데이트한 사람 이름(로그인 계정과 무관하게 직접 입력)", example = "김레이")
    private String updatedBy;

    @Schema(description = "등록일(선택, 미입력 시 오늘)", example = "2026-08-04")
    private LocalDate releasedAt;
}
