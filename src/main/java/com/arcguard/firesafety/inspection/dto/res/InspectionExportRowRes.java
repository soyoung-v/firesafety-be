package com.arcguard.firesafety.inspection.dto.res;

import com.arcguard.firesafety.inspection.model.InspectionResultType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "점검 이력 엑셀 다운로드 한 행")
public class InspectionExportRowRes {

    @Schema(description = "점검 실행 ID", example = "1")
    private Long inspectionId;
    @Schema(description = "현장명", example = "아크타워1호점")
    private String siteName;
    @Schema(description = "분전반명", example = "2층 분전반")
    private String panelName;
    @Schema(description = "분전반 No", example = "M-001")
    private String mNo;
    @Schema(description = "점검 일시", example = "2026-07-29T10:00:00")
    private LocalDateTime inspectedAt;
    @Schema(description = "점검자 이름", example = "홍길동")
    private String inspectorName;
    @Schema(description = "점검 항목 ID", example = "1")
    private Long itemId;
    @Schema(description = "점검 항목명", example = "누전차단기 동작 확인")
    private String itemName;
    @Schema(description = "점검 항목 설명", example = "테스트 버튼으로 정상 차단되는지 확인")
    private String description;
    @Schema(description = "항목별 점검 결과", example = "NORMAL")
    private InspectionResultType result;
    @Schema(description = "특이사항/비고", example = "3번 회로 접점 마모 확인됨")
    private String note;
}
