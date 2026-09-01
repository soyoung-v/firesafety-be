package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.dto.req.AlertListReq;
import com.arcguard.firesafety.alert.dto.res.AlertExportRes;
import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
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

class AlertExcelServiceTest {

    @Test
    @DisplayName("API-023: 경보 이력 엑셀 양식에 제목, 조건, 헤더, 데이터가 작성된다")
    void createAlertHistoryExcel() throws Exception {
        // given
        Clock fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 7, 23, 13, 30)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        AlertExcelService alertExcelService = new AlertExcelService(fixedClock);

        AlertListReq req = new AlertListReq();
        req.setFrom(LocalDate.of(2026, 7, 1));
        req.setTo(LocalDate.of(2026, 7, 21));

        // when
        byte[] excel = alertExcelService.createAlertHistoryExcel(List.of(alertExportRes()), req);

        // then
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheet("알림이력");

            assertThat(sheet.getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo("ArcGuard - 알림(경보) 이력");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue())
                    .isEqualTo("기간 2026-07-01 ~ 2026-07-21, 현장 전체");
            assertThat(sheet.getRow(3).getCell(2).getStringCellValue())
                    .isEqualTo("2026-07-23 13:30:00");

            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("번호");
            assertThat(sheet.getRow(5).getCell(12).getStringCellValue()).isEqualTo("조치자");
            assertThat(sheet.getRow(5).getCell(13).getStringCellValue()).isEqualTo("비고");
            assertThat(sheet.getRow(6).getCell(3).getStringCellValue()).isEqualTo("아크타워01");
            assertThat(sheet.getRow(6).getCell(6).getStringCellValue()).isEqualTo("아크");
            assertThat(sheet.getRow(6).getCell(7).getStringCellValue()).isEqualTo("하드웨어");
            assertThat(sheet.getRow(6).getCell(8).getStringCellValue()).isEqualTo("미확인");
            assertThat(sheet.getRow(6).getCell(12).getStringCellValue()).isEqualTo("홍길동");
            assertThat(sheet.getRow(6).getCell(13).getStringCellValue()).isEqualTo("케이블 재접속");
        }
    }

    private AlertExportRes alertExportRes() {
        AlertExportRes res = new AlertExportRes();
        res.setAlertId(1L);
        res.setSiteName("아크타워01");
        res.setPanelName("아크타워01-분전반A");
        res.setCircuitNo(3);
        res.setType(AlertType.ARC);
        res.setSource(AlertSource.DEVICE);
        res.setStatus(AlertStatus.UNCONFIRMED);
        res.setTriggeredAt(LocalDateTime.of(2026, 7, 21, 9, 12, 33));
        res.setResolvedByName("홍길동");
        res.setResolutionNote("케이블 재접속");
        return res;
    }
}
