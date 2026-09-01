package com.arcguard.firesafety.alert.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "미처리 조치 목록(페이지) (REQ-306)")
public class AlertPendingPageRes {

    @Schema(description = "이번 페이지 항목 목록")
    private List<AlertPendingRes> content;
    @Schema(description = "전체 항목 수", example = "12")
    private long totalElements;
}
