package com.arcguard.firesafety.facility.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "현장 담당 직원 목록(페이지)")
public class SiteManagedUserPageRes {

    @Schema(description = "이번 페이지 항목 목록")
    private List<SiteManagedUserRes> content;
    @Schema(description = "전체 항목 수", example = "12")
    private long totalElements;
}
