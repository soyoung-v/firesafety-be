package com.arcguard.firesafety.inspection.service;

import com.arcguard.firesafety.inspection.dto.req.InspectionHistoryListReq;
import com.arcguard.firesafety.inspection.dto.res.InspectionExportRowRes;
import com.arcguard.firesafety.inspection.model.InspectionResultType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InspectionExcelServiceTest {

    @Test
    @DisplayName("API-523: 점검 이력 엑셀 양식에 제목, 조건, 헤더, 항목별 데이터가 작성된다")
    void createInspectionHistoryExcel() throws Exception {
        // given
        Clock fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 8, 4, 10, 30)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        InspectionExcelService inspectionExcelService = new InspectionExcelService(fixedClock);

        InspectionHistoryListReq req = new InspectionHistoryListReq();
        req.setFrom(LocalDate.of(2026, 8, 1));
        req.setTo(LocalDate.of(2026, 8, 4));

        // when
        byte[] excel = inspectionExcelService.createInspectionHistoryExcel(List.of(exportRowRes()), req);

        // then
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheet("점검이력");

            assertThat(sheet.getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo("ArcGuard - 설비 점검 이력");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue())
                    .isEqualTo("기간 2026-08-01 ~ 2026-08-04, 현장 아크타워1호점, 분전반 2층 분전반");
            assertThat(sheet.getRow(3).getCell(2).getStringCellValue())
                    .isEqualTo("2026-08-04 10:30:00");

            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("번호");
            assertThat(sheet.getRow(5).getCell(7).getStringCellValue()).isEqualTo("점검항목");
            assertThat(sheet.getRow(5).getCell(10).getStringCellValue()).isEqualTo("비고");
            assertThat(sheet.getRow(6).getCell(3).getStringCellValue()).isEqualTo("아크타워1호점");
            assertThat(sheet.getRow(6).getCell(5).getStringCellValue()).isEqualTo("00001");
            assertThat(sheet.getRow(6).getCell(7).getStringCellValue()).isEqualTo("누전차단기 동작 확인");
            assertThat(sheet.getRow(6).getCell(9).getStringCellValue()).isEqualTo("정상");
            assertThat(sheet.getRow(6).getCell(10).getStringCellValue()).isEqualTo("이상 없음");
        }
    }

    private InspectionExportRowRes exportRowRes() {
        InspectionExportRowRes res = new InspectionExportRowRes();
        res.setInspectionId(1L);
        res.setSiteName("아크타워1호점");
        res.setPanelName("2층 분전반");
        res.setMNo("00001");
        res.setInspectedAt(LocalDateTime.of(2026, 8, 4, 9, 0));
        res.setInspectorName("박직원");
        res.setItemId(100L);
        res.setItemName("누전차단기 동작 확인");
        res.setDescription("테스트 버튼으로 정상 차단되는지 확인");
        res.setResult(InspectionResultType.NORMAL);
        res.setNote("이상 없음");
        return res;
    }
}
