package com.arcguard.firesafety.alert.controller;

import com.arcguard.firesafety.alert.dto.req.AlertBulkActionReq;
import com.arcguard.firesafety.alert.dto.req.AlertListReq;
import com.arcguard.firesafety.alert.dto.req.AlertPendingReq;
import com.arcguard.firesafety.alert.dto.req.AlertResolveReq;
import com.arcguard.firesafety.alert.dto.res.AlertBulkActionRes;
import com.arcguard.firesafety.alert.dto.res.AlertListPageRes;
import com.arcguard.firesafety.alert.dto.res.AlertPendingPageRes;
import com.arcguard.firesafety.alert.service.AlertService;
import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "경보알림", description = "경보 이력 조회, 확인, 조치완료, 엑셀 다운로드")
public class AlertController {

    private final AlertService alertService;

    // 경보 목록 조회 (GET /api/alerts)
    // SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장 경보만 조회
    @Operation(summary = "경보 목록 조회", description = "기간/현장/유형/상태 필터를 지원한다. SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장 경보만 조회한다.")
    @GetMapping
    public ResultResponse<AlertListPageRes> getAlerts(@ModelAttribute AlertListReq req) {
        AlertListPageRes alerts = alertService.getAlerts(req);
        return ResultResponse.success(String.format("%d rows", alerts.getContent().size()), alerts);
    }

    // 미처리 조치 목록 조회 (GET /api/alerts/pending, REQ-306)
    // 기간 필터 없이 UNCONFIRMED/CONFIRMED 경보만 모아서 보여준다. 지연 판단(elapsedHours)은 서버가 계산해서 내려준다
    @Operation(summary = "미처리 조치 목록 조회", description = "기간 필터 없이 UNCONFIRMED/CONFIRMED 상태 경보만 조회한다. 각 항목의 elapsedHours로 지연 여부를 판단한다.")
    @GetMapping("/pending")
    public ResultResponse<AlertPendingPageRes> getPendingAlerts(@ModelAttribute AlertPendingReq req) {
        AlertPendingPageRes alerts = alertService.getPendingAlerts(req);
        return ResultResponse.success(String.format("%d rows", alerts.getContent().size()), alerts);
    }

    // 경보 이력 엑셀 다운로드 (GET /api/alerts/export)
    // ADMIN 이상만 전체/선택 경보 처리내역을 xlsx 파일로 다운로드
    @Operation(summary = "경보 이력 엑셀 다운로드", description = "ADMIN 이상 가능. 필터 조건 전체 또는 alertIds 선택 경보 처리내역을 xlsx 파일로 다운로드한다.")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAlerts(@ModelAttribute AlertListReq req) {
        byte[] excel = alertService.exportAlerts(req);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("알림이력.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // 경보 확인 처리 (PATCH /api/alerts/{alertId}/confirm)
    // UNCONFIRMED 상태만 CONFIRMED로 전환
    @Operation(summary = "경보 확인 처리", description = "UNCONFIRMED 상태의 경보만 CONFIRMED로 전환한다.")
    @PatchMapping("/{alertId}/confirm")
    public ResultResponse<Void> confirmAlert(@PathVariable Long alertId) {
        alertService.confirmAlert(alertId);
        return ResultResponse.success("경보 확인 성공", null);
    }

    // 경보 조치완료 처리 (PATCH /api/alerts/{alertId}/resolve)
    // CONFIRMED 상태만 RESOLVED로 전환하고, 선택 입력한 비고를 저장
    @Operation(summary = "경보 조치완료 처리", description = "CONFIRMED 상태의 경보만 RESOLVED로 전환한다. 선택 입력한 resolutionNote는 엑셀 비고 컬럼에 출력된다.")
    @PatchMapping("/{alertId}/resolve")
    public ResultResponse<Void> resolveAlert(@PathVariable Long alertId,
                                             @Valid @RequestBody(required = false) AlertResolveReq req) {
        alertService.resolveAlert(alertId, req);
        return ResultResponse.success("경보 조치완료 성공", null);
    }

    // 경보 일괄 확인 처리 (PATCH /api/alerts/bulk-confirm)
    // 대상이 몇 건이든 WS 브로드캐스트는 현장당 한 번만 나간다(건별 반복 호출 시의 리페치 폭주 방지)
    @Operation(summary = "경보 일괄 확인 처리", description = "UNCONFIRMED 상태인 대상만 CONFIRMED로 전환한다. 대상이 여러 건이어도 실시간 갱신 신호는 현장당 한 번만 전송된다.")
    @PatchMapping("/bulk-confirm")
    public ResultResponse<AlertBulkActionRes> bulkConfirmAlerts(@RequestBody AlertBulkActionReq req) {
        AlertBulkActionRes result = alertService.bulkConfirmAlerts(req);
        return ResultResponse.success(String.format("성공 %d건, 실패 %d건", result.getSuccessCount(), result.getFailCount()), result);
    }

    // 경보 일괄 조치완료 처리 (PATCH /api/alerts/bulk-resolve)
    @Operation(summary = "경보 일괄 조치완료 처리", description = "CONFIRMED 상태인 대상만 RESOLVED로 전환한다. 개별 비고는 받지 않는다.")
    @PatchMapping("/bulk-resolve")
    public ResultResponse<AlertBulkActionRes> bulkResolveAlerts(@RequestBody AlertBulkActionReq req) {
        AlertBulkActionRes result = alertService.bulkResolveAlerts(req);
        return ResultResponse.success(String.format("성공 %d건, 실패 %d건", result.getSuccessCount(), result.getFailCount()), result);
    }
}
