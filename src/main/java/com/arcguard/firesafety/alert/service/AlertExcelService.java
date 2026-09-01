package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.dto.req.AlertListReq;
import com.arcguard.firesafety.alert.dto.res.AlertExportRes;
import com.arcguard.firesafety.alert.exception.AlertErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertExcelService {

    private static final String SHEET_NAME = "알림이력";
    private static final String TITLE = "ArcGuard - 알림(경보) 이력";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {
            "번호", "발생일시", "현장명", "분전반명(장비명)", "회로(채널)", "경보유형",
            "판정소스", "상태", "확인자", "확인일시", "조치일시", "조치자", "비고"
    };

    private final Clock clock;

    // 경보 이력 엑셀 생성
    // 1. 제목/조회조건 작성 → 2. 표 헤더 작성 → 3. 경보 rows 작성 → 4. xlsx bytes 반환
    public byte[] createAlertHistoryExcel(List<AlertExportRes> alerts, AlertListReq req) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            AlertExcelStyles styles = createStyles(workbook);

            writeTitle(sheet, styles);
            writeSearchInfo(sheet, req, alerts, styles);
            writeHeader(sheet, styles);
            writeRows(sheet, alerts, styles);
            adjustColumns(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(AlertErrorCode.ALERT_EXPORT_FAILED);
        }
    }

    // 제목은 샘플 양식처럼 B~M 영역을 합쳐서 표시
    private void writeTitle(Sheet sheet, AlertExcelStyles styles) {
        Row titleRow = sheet.createRow(1);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(TITLE);
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 13));
    }

    // 조회 조건과 다운로드 시각 표시
    private void writeSearchInfo(Sheet sheet, AlertListReq req, List<AlertExportRes> alerts, AlertExcelStyles styles) {
        Row conditionRow = sheet.createRow(2);
        Cell conditionCell = conditionRow.createCell(2);
        conditionCell.setCellValue(buildConditionText(req, alerts));
        conditionCell.setCellStyle(styles.metaStyle());

        Row downloadRow = sheet.createRow(3);
        Cell downloadCell = downloadRow.createCell(2);
        downloadCell.setCellValue(LocalDateTime.now(clock).format(DATE_TIME_FORMATTER));
        downloadCell.setCellStyle(styles.metaStyle());
    }

    // 표 컬럼명 작성
    private void writeHeader(Sheet sheet, AlertExcelStyles styles) {
        Row headerRow = sheet.createRow(5);
        headerRow.setHeightInPoints(22);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(styles.headerStyle());
        }
    }

    // 경보 목록을 샘플 양식 컬럼 순서대로 작성
    private void writeRows(Sheet sheet, List<AlertExportRes> alerts, AlertExcelStyles styles) {
        for (int i = 0; i < alerts.size(); i++) {
            AlertExportRes alert = alerts.get(i);
            Row row = sheet.createRow(i + 6);
            writeCell(row, 1, i + 1, styles.bodyStyle());
            writeCell(row, 2, formatDateTime(alert.getTriggeredAt()), styles.bodyStyle());
            writeCell(row, 3, alert.getSiteName(), styles.bodyStyle());
            writeCell(row, 4, alert.getPanelName(), styles.bodyStyle());
            writeCell(row, 5, formatCircuitNo(alert.getCircuitNo()), styles.bodyStyle());
            writeCell(row, 6, alert.getType() == null ? "" : alert.getType().getLabel(), styles.bodyStyle());
            writeCell(row, 7, alert.getSource() == null ? "" : alert.getSource().getLabel(), styles.bodyStyle());
            writeCell(row, 8, alert.getStatus() == null ? "" : alert.getStatus().getLabel(), styles.bodyStyle());
            writeCell(row, 9, alert.getConfirmedByName(), styles.bodyStyle());
            writeCell(row, 10, formatDateTime(alert.getConfirmedAt()), styles.bodyStyle());
            writeCell(row, 11, formatDateTime(alert.getResolvedAt()), styles.bodyStyle());
            writeCell(row, 12, alert.getResolvedByName(), styles.bodyStyle());
            writeCell(row, 13, alert.getResolutionNote(), styles.bodyStyle());
        }
    }

    // 문자열/숫자 모두 같은 표 스타일로 작성
    private void writeCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    // 조회 기간과 현장 조건 요약
    private String buildConditionText(AlertListReq req, List<AlertExportRes> alerts) {
        return "기간 " + formatPeriod(req.getFrom(), req.getTo()) + ", " + formatSite(req, alerts);
    }

    // 기간 미입력도 전체 기간으로 표시
    private String formatPeriod(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "전체";
        }
        String fromText = from == null ? "시작일 전체" : from.format(DATE_FORMATTER);
        String toText = to == null ? "종료일 전체" : to.format(DATE_FORMATTER);
        return fromText + " ~ " + toText;
    }

    // siteId 필터가 있으면 첫 row의 현장명을 우선 사용
    private String formatSite(AlertListReq req, List<AlertExportRes> alerts) {
        if (req.getSiteId() == null) {
            return "현장 전체";
        }
        return alerts.stream()
                .map(AlertExportRes::getSiteName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .map(name -> "현장 " + name)
                .orElse("현장 ID " + req.getSiteId());
    }

    // 회로 없는 시스템 경보는 샘플처럼 '-' 표시
    private String formatCircuitNo(Integer circuitNo) {
        return circuitNo == null ? "-" : String.valueOf(circuitNo);
    }

    // 날짜값이 없는 확인/조치일시는 빈칸
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    // 샘플 양식에 맞춰 컬럼 폭 지정
    private void adjustColumns(Sheet sheet) {
        int[] widths = {8, 20, 20, 24, 12, 14, 14, 12, 14, 20, 20, 14, 24};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i + 1, widths[i] * 256);
        }
    }

    // 엑셀 스타일 모음 생성
    private AlertExcelStyles createStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle metaStyle = workbook.createCellStyle();
        metaStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(headerStyle);

        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setAlignment(HorizontalAlignment.CENTER);
        bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(bodyStyle);

        return new AlertExcelStyles(titleStyle, metaStyle, headerStyle, bodyStyle);
    }

    // 표 영역은 테두리로 읽기 쉽게 구분
    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private record AlertExcelStyles(CellStyle titleStyle, CellStyle metaStyle, CellStyle headerStyle, CellStyle bodyStyle) {
    }
}
