package com.arcguard.firesafety.inspection.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.inspection.dto.req.InspectionHistoryListReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionItemApplyReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionItemCreateReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionSaveReq;
import com.arcguard.firesafety.inspection.dto.res.InspectionHistoryPageRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionItemCreateRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionItemRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionSaveRes;
import com.arcguard.firesafety.inspection.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비점검", description = "현장 점검 항목 카탈로그, 분전반 적용, 체크리스트 저장, 점검 이력 조회 (REQ-511, REQ-512)")
public class InspectionController {

    private final InspectionService inspectionService;

    // 점검 항목 등록 (POST /api/sites/{siteId}/inspection-items, ADMIN 이상) — 분전반이 아니라 현장 카탈로그에 등록
    @Operation(summary = "점검 항목 등록", description = "현장 점검 항목 카탈로그에 항목을 등록한다. ADMIN 이상만 가능하다.")
    @PostMapping("/sites/{siteId}/inspection-items")
    public ResultResponse<InspectionItemCreateRes> createItem(@PathVariable Long siteId,
                                                              @RequestBody InspectionItemCreateReq req) {
        InspectionItemCreateRes res = inspectionService.createItem(siteId, req);
        return ResultResponse.success("점검 항목 등록 성공", res);
    }

    // 현장 점검 항목 카탈로그 조회 (GET /api/sites/{siteId}/inspection-items, GENERAL 이상)
    @Operation(summary = "현장 점검 항목 카탈로그 조회", description = "현장에 등록된 점검 항목 전체를 조회한다. 분전반에 적용할 항목을 고를 때 후보 목록으로 쓴다.")
    @GetMapping("/sites/{siteId}/inspection-items")
    public ResultResponse<List<InspectionItemRes>> getSiteItems(@PathVariable Long siteId) {
        List<InspectionItemRes> items = inspectionService.getSiteItems(siteId);
        return ResultResponse.success(String.format("%d rows", items.size()), items);
    }

    // 점검 항목 수정 (PATCH /api/sites/{siteId}/inspection-items/{itemId}, ADMIN 이상)
    @Operation(summary = "점검 항목 수정", description = "현장 점검 항목 카탈로그의 항목명/설명을 수정한다. ADMIN 이상만 가능하다.")
    @PatchMapping("/sites/{siteId}/inspection-items/{itemId}")
    public ResultResponse<Void> updateItem(@PathVariable Long siteId, @PathVariable Long itemId,
                                           @RequestBody InspectionItemCreateReq req) {
        inspectionService.updateItem(siteId, itemId, req);
        return ResultResponse.success("점검 항목 수정 성공", null);
    }

    // 점검 항목 삭제 (DELETE /api/sites/{siteId}/inspection-items/{itemId}, ADMIN 이상)
    @Operation(summary = "점검 항목 삭제", description = "현장 점검 항목 카탈로그에서 항목을 삭제한다. 이미 분전반에 적용됐거나 점검 결과에 쓰인 항목은 삭제할 수 없다.")
    @DeleteMapping("/sites/{siteId}/inspection-items/{itemId}")
    public ResultResponse<Void> deleteItem(@PathVariable Long siteId, @PathVariable Long itemId) {
        inspectionService.deleteItem(siteId, itemId);
        return ResultResponse.success("점검 항목 삭제 성공", null);
    }

    // 분전반에 점검 항목 일괄 적용 (POST /api/panels/{panelId}/inspection-items, GENERAL 이상) — 요청 목록으로 전체교체
    @Operation(summary = "분전반 점검 항목 적용", description = "현장 카탈로그 중 이 분전반에 적용할 항목을 전체교체(delete-then-insert)한다. GENERAL 이상 가능하다.")
    @PostMapping("/panels/{panelId}/inspection-items")
    public ResultResponse<Void> applyItems(@PathVariable Long panelId, @RequestBody InspectionItemApplyReq req) {
        inspectionService.applyItems(panelId, req);
        return ResultResponse.success("점검 항목 적용 성공", null);
    }

    // 분전반에 적용된 점검 항목 목록 조회 (GET /api/panels/{panelId}/inspection-items, GENERAL 이상)
    @Operation(summary = "분전반 점검 항목 목록 조회", description = "분전반에 적용된 점검 항목 목록을 조회한다. GENERAL 이상 가능하다.")
    @GetMapping("/panels/{panelId}/inspection-items")
    public ResultResponse<List<InspectionItemRes>> getItems(@PathVariable Long panelId) {
        List<InspectionItemRes> items = inspectionService.getItems(panelId);
        return ResultResponse.success(String.format("%d rows", items.size()), items);
    }

    // 점검 체크리스트 저장 (POST /api/panels/{panelId}/inspections)
    @Operation(summary = "점검 체크리스트 저장", description = "분전반 점검 1회 실행 결과를 항목별로 저장한다.")
    @PostMapping("/panels/{panelId}/inspections")
    public ResultResponse<InspectionSaveRes> saveChecklist(@PathVariable Long panelId,
                                                           @RequestBody InspectionSaveReq req) {
        InspectionSaveRes res = inspectionService.saveChecklist(panelId, req);
        return ResultResponse.success("점검 결과 저장 성공", res);
    }

    // 점검 이력 조회 (GET /api/panels/{panelId}/inspections)
    @Operation(summary = "점검 이력 조회", description = "분전반의 점검 이력을 기간 필터로 조회한다.")
    @GetMapping("/panels/{panelId}/inspections")
    public ResultResponse<InspectionHistoryPageRes> getHistory(@PathVariable Long panelId,
                                                               @ModelAttribute InspectionHistoryListReq req) {
        InspectionHistoryPageRes res = inspectionService.getHistory(panelId, req);
        return ResultResponse.success(String.format("%d rows", res.getContent().size()), res);
    }

    // 점검 이력 엑셀 다운로드 (GET /api/panels/{panelId}/inspections/export)
    // GENERAL 이상이 본인 접근 현장 분전반의 점검 이력을 xlsx 파일로 다운로드
    @Operation(summary = "점검 이력 엑셀 다운로드", description = "GENERAL 이상 가능. 기간 필터 조건의 점검 이력을 항목별 row로 펼쳐 xlsx 파일로 다운로드한다.")
    @GetMapping("/panels/{panelId}/inspections/export")
    public ResponseEntity<byte[]> exportHistory(@PathVariable Long panelId,
                                                @ModelAttribute InspectionHistoryListReq req) {
        byte[] excel = inspectionService.exportHistory(panelId, req);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("점검이력.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
