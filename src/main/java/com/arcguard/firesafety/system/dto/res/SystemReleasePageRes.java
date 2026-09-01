package com.arcguard.firesafety.system.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "업데이트 이력 목록(페이지)")
public class SystemReleasePageRes {

    @Schema(description = "이번 페이지 항목 목록")
    private List<SystemVersionHistoryRes> content;
    @Schema(description = "전체 항목 수", example = "23")
    private long totalElements;
    @Schema(description = "페이지 번호(0부터)", example = "0")
    private int page;
    @Schema(description = "페이지 크기", example = "11")
    private int size;
}
