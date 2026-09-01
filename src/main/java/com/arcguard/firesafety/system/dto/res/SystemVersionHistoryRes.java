package com.arcguard.firesafety.system.dto.res;

import com.arcguard.firesafety.system.model.SystemReleaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "업데이트 이력 1건")
public class SystemVersionHistoryRes {

    @Schema(description = "릴리즈 버전", example = "1.2.0")
    private String version;
    @Schema(description = "구분(SOFTWARE/MODEL)", example = "SOFTWARE")
    private SystemReleaseType type;
    @Schema(description = "구분 한글 라벨", example = "소프트웨어")
    private String typeLabel;
    @Schema(description = "변경 내용")
    private String description;
    @Schema(description = "업데이트한 사람 이름")
    private String updatedBy;
    @Schema(description = "등록일", example = "2026-08-04T10:20:00")
    private LocalDateTime releasedAt;

    public static SystemVersionHistoryRes from(com.arcguard.firesafety.system.model.SystemReleaseHistory release) {
        return new SystemVersionHistoryRes(
                release.getVersion(),
                release.getType(),
                release.getType().getLabel(),
                release.getDescription(),
                release.getUpdatedBy(),
                release.getReleasedAt()
        );
    }
}
