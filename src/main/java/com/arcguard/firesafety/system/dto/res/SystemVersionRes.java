package com.arcguard.firesafety.system.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "SW 버전 정보 요약 (REQ-702) — 등록된 이력 중 최신 소프트웨어/AI 모델 버전")
public class SystemVersionRes {

    @Schema(description = "현재 소프트웨어 버전(유의적 버전). 등록된 이력이 없으면 null", example = "1.2.0")
    private String version;
    @Schema(description = "현재 소프트웨어 버전의 등록일. 등록된 이력이 없으면 null", example = "2026-08-04T10:20:00")
    private LocalDateTime lastUpdatedAt;
    @Schema(description = "현재 AI 모델 버전. 등록된 이력이 없으면 null", example = "2.3.1")
    private String modelVersion;
}
