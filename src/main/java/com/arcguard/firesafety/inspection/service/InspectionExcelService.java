package com.arcguard.firesafety.inspection.service;

import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.inspection.dto.req.InspectionHistoryListReq;
import com.arcguard.firesafety.inspection.dto.res.InspectionExportRowRes;
import com.arcguard.firesafety.inspection.exception.InspectionErrorCode;
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
public class InspectionExcelService {

    private static final String SHEET_NAME = "점검이력";
    private static final String TITLE = "ArcGuard - 설비 점검 이력";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {
            "번호", "점검일시", "현장명", "분전반명", "분전반No", "점검자", "점검항목", "항목설명", "결과", "비고"
    };

    private final Clock clock;

    // 점검 이력 엑셀 생성
    // 1. 제목/조회조건 작성 → 2. 표 헤더 작성 → 3. 점검 항목별 rows 작성 → 4. xlsx bytes 반환
    public byte[] createInspectionHistoryExcel(List<InspectionExportRowRes> rows, InspectionHistoryListReq req) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            InspectionExcelStyles styles = createStyles(workbook);

            writeTitle(sheet, styles);
            writeSearchInfo(sheet, req, rows, styles);
            writeHeader(sheet, styles);
            writeRows(sheet, rows, styles);
            adjustColumns(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(InspectionErrorCode.INSPECTION_EXPORT_FAILED);
        }
    }

    // 제목은 B~K 영역을 합쳐서 표시
    private void writeTitle(Sheet sheet, InspectionExcelStyles styles) {
        Row titleRow = sheet.createRow(1);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(TITLE);
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 10));
    }

    // 조회 조건과 다운로드 시각 표시
    private void writeSearchInfo(Sheet sheet, InspectionHistoryListReq req, List<InspectionExportRowRes> rows, InspectionExcelStyles styles) {
        Row conditionRow = sheet.createRow(2);
        Cell conditionCell = conditionRow.createCell(2);
        conditionCell.setCellValue(buildConditionText(req, rows));
        conditionCell.setCellStyle(styles.metaStyle());

        Row downloadRow = sheet.createRow(3);
        Cell downloadCell = downloadRow.createCell(2);
        downloadCell.setCellValue(LocalDateTime.now(clock).format(DATE_TIME_FORMATTER));
        downloadCell.setCellStyle(styles.metaStyle());
    }

    // 표 컬럼명 작성
    private void writeHeader(Sheet sheet, InspectionExcelStyles styles) {
        Row headerRow = sheet.createRow(5);
        headerRow.setHeightInPoints(22);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(styles.headerStyle());
        }
    }

    // 점검 이력을 항목별 행으로 펼쳐 작성
    private void writeRows(Sheet sheet, List<InspectionExportRowRes> rows, InspectionExcelStyles styles) {
        for (int i = 0; i < rows.size(); i++) {
            InspectionExportRowRes rowData = rows.get(i);
            Row row = sheet.createRow(i + 6);
            writeCell(row, 1, i + 1, styles.bodyStyle());
            writeCell(row, 2, formatDateTime(rowData.getInspectedAt()), styles.bodyStyle());
            writeCell(row, 3, rowData.getSiteName(), styles.bodyStyle());
            writeCell(row, 4, rowData.getPanelName(), styles.bodyStyle());
            writeCell(row, 5, rowData.getMNo(), styles.bodyStyle());
            writeCell(row, 6, rowData.getInspectorName(), styles.bodyStyle());
            writeCell(row, 7, rowData.getItemName(), styles.bodyStyle());
            writeCell(row, 8, rowData.getDescription(), styles.bodyStyle());
            writeCell(row, 9, rowData.getResult() == null ? "" : rowData.getResult().getLabel(), styles.bodyStyle());
            writeCell(row, 10, rowData.getNote(), styles.bodyStyle());
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

    // 조회 기간과 분전반 조건 요약
    private String buildConditionText(InspectionHistoryListReq req, List<InspectionExportRowRes> rows) {
        return "기간 " + formatPeriod(req.getFrom(), req.getTo()) + ", " + formatPanel(rows);
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

    // 첫 row 기준으로 현장/분전반명을 표시
    private String formatPanel(List<InspectionExportRowRes> rows) {
        return rows.stream()
                .findFirst()
                .map(row -> "현장 " + blankToDash(row.getSiteName()) + ", 분전반 " + blankToDash(row.getPanelName()))
                .orElse("조회 결과 없음");
    }

    // 빈 문자열은 엑셀 조건 표시에서 '-'로 대체
    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    // 날짜값이 없는 점검일시는 빈칸
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    // 점검 이력 양식에 맞춰 컬럼 폭 지정
    private void adjustColumns(Sheet sheet) {
        int[] widths = {8, 20, 20, 24, 14, 14, 28, 34, 12, 36};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i + 1, widths[i] * 256);
        }
    }

    // 엑셀 스타일 모음 생성
    private InspectionExcelStyles createStyles(Workbook workbook) {
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

        return new InspectionExcelStyles(titleStyle, metaStyle, headerStyle, bodyStyle);
    }

    // 표 영역은 테두리로 읽기 쉽게 구분
    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private record InspectionExcelStyles(CellStyle titleStyle, CellStyle metaStyle, CellStyle headerStyle, CellStyle bodyStyle) {
    }
}
