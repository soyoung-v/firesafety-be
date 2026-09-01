package com.arcguard.firesafety.diagnosis.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "AI 진단 결과 페이지 응답")
public class DiagnosisResultPageRes {

    @Schema(description = "AI 진단 결과 목록")
    private List<DiagnosisResultRes> content;

    @Schema(description = "전체 건수", example = "35")
    private long totalElements;

    @Schema(description = "현재 페이지 번호. 0부터 시작", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;
}
