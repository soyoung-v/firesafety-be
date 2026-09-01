package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.dto.req.AlertBulkActionReq;
import com.arcguard.firesafety.alert.dto.req.AlertListReq;
import com.arcguard.firesafety.alert.dto.req.AlertPendingReq;
import com.arcguard.firesafety.alert.dto.req.AlertResolveReq;
import com.arcguard.firesafety.alert.dto.res.AlertBulkActionRes;
import com.arcguard.firesafety.alert.dto.res.AlertExportRes;
import com.arcguard.firesafety.alert.dto.res.AlertListPageRes;
import com.arcguard.firesafety.alert.dto.res.AlertListRes;
import com.arcguard.firesafety.alert.dto.res.AlertPendingPageRes;
import com.arcguard.firesafety.alert.dto.res.AlertPendingRes;
import com.arcguard.firesafety.alert.exception.AlertErrorCode;
import com.arcguard.firesafety.alert.mapper.AlertMapper;
import com.arcguard.firesafety.alert.model.Alert;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AlertMapper alertMapper;
    private final AlertNotificationPublisher alertNotificationPublisher;
    private final AlertExcelService alertExcelService;

    // 경보 목록 조회
    // 1. 현재 사용자 확인 → 2. 역할별 조회 범위 계산 → 3. 필터/페이징 계산 → 4. 목록/개수 조회
    @Transactional(readOnly = true)
    public AlertListPageRes getAlerts(AlertListReq req) {
        UserPrincipal actor = getCurrentUser();
        AlertListReq searchReq = normalizeReq(req);
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;

        validateDateRange(searchReq);
        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());

        String status = searchReq.getStatus() == null ? null : searchReq.getStatus().name();
        String type = searchReq.getType() == null ? null : searchReq.getType().name();

        List<AlertListRes> content = alertMapper.findAlerts(
                actor.getUserId(),
                superAdmin,
                status,
                type,
                searchReq.getSiteId(),
                searchReq.getPanelId(),
                fromAt,
                toAt,
                size,
                offset
        );
        long totalElements = alertMapper.countAlerts(
                actor.getUserId(),
                superAdmin,
                status,
                type,
                searchReq.getSiteId(),
                searchReq.getPanelId(),
                fromAt,
                toAt
        );

        return new AlertListPageRes(content, totalElements, page, size);
    }

    // 미처리 조치 목록 조회 (REQ-306)
    // 과거엔 프론트가 /alerts를 기간 무관 전체 스캔해서 이 화면을 만들다가 성능 문제로 롤백된 적이 있어서,
    // 이번엔 필터링과 경과시간 계산을 전부 쿼리에서 처리해 페이지 단위로만 내려준다.
    // 1. 현재 사용자 확인 → 2. 페이징 계산 → 3. 목록/개수 조회
    @Transactional(readOnly = true)
    public AlertPendingPageRes getPendingAlerts(AlertPendingReq req) {
        UserPrincipal actor = getCurrentUser();
        AlertPendingReq searchReq = normalizePendingReq(req);
        int page = resolvePendingPage(searchReq);
        int size = resolvePendingSize(searchReq);
        int offset = page * size;
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());

        List<AlertPendingRes> content = alertMapper.findPendingAlerts(
                actor.getUserId(),
                superAdmin,
                searchReq.getSiteId(),
                size,
                offset
        );
        long totalElements = alertMapper.countPendingAlerts(actor.getUserId(), superAdmin, searchReq.getSiteId());

        return new AlertPendingPageRes(content, totalElements);
    }

    // null 요청도 기본 목록 조회로 처리
    private AlertPendingReq normalizePendingReq(AlertPendingReq req) {
        return req == null ? new AlertPendingReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePendingPage(AlertPendingReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolvePendingSize(AlertPendingReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 경보 이력 엑셀 다운로드
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 필터/선택 ID 검증 → 4. 엑셀 row 조회 → 5. xlsx 생성
    @Transactional(readOnly = true)
    public byte[] exportAlerts(AlertListReq req) {
        UserPrincipal actor = getCurrentUser();
        validateAdminOrSuperAdmin(actor);

        AlertListReq searchReq = normalizeReq(req);
        validateDateRange(searchReq);
        validateAlertIds(searchReq.getAlertIds());

        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());

        String status = searchReq.getStatus() == null ? null : searchReq.getStatus().name();
        String type = searchReq.getType() == null ? null : searchReq.getType().name();

        List<AlertExportRes> rows = alertMapper.findAlertExportRows(
                actor.getUserId(),
                superAdmin,
                status,
                type,
                searchReq.getSiteId(),
                fromAt,
                toAt,
                searchReq.getAlertIds()
        );

        return alertExcelService.createAlertHistoryExcel(rows, searchReq);
    }

    // 경보 확인 처리
    // 1. 현재 사용자 확인 → 2. 권한 범위 안의 경보 조회 → 3. UNCONFIRMED 확인 → 4. CONFIRMED 전환
    @Transactional
    public void confirmAlert(Long alertId) {
        UserPrincipal actor = getCurrentUser();
        Alert alert = findAccessibleAlert(actor, alertId);
        validateCanConfirm(alert);

        int updatedRows = alertMapper.confirmAlert(alertId, actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AlertErrorCode.ALERT_CANNOT_CONFIRM);
        }
        alertNotificationPublisher.publishStatusChanged(alert, AlertStatus.CONFIRMED);
    }

    // 경보 조치완료 처리
    // 1. 현재 사용자 확인 → 2. 권한 범위 안의 경보 조회 → 3. CONFIRMED 확인 → 4. 비고 정리 → 5. RESOLVED 전환
    @Transactional
    public void resolveAlert(Long alertId, AlertResolveReq req) {
        UserPrincipal actor = getCurrentUser();
        Alert alert = findAccessibleAlert(actor, alertId);
        validateCanResolve(alert);

        String resolutionNote = normalizeResolutionNote(req);
        int updatedRows = alertMapper.resolveAlert(alertId, actor.getUserId(), resolutionNote);
        if (updatedRows == 0) {
            throw new BusinessException(AlertErrorCode.ALERT_NOT_CONFIRMED);
        }
        alertNotificationPublisher.publishStatusChanged(alert, AlertStatus.RESOLVED);
    }

    // 기존 내부 호출이 생겨도 비고 없이 조치완료할 수 있게 유지
    public void resolveAlert(Long alertId) {
        resolveAlert(alertId, null);
    }

    // 경보 일괄 확인 처리 — 대상이 몇 건이든 WS 브로드캐스트는 현장당 한 번만 나간다(성능/부하 대응).
    // 1. 대상 ID 검증 → 2. 건별로 접근권한/상태 확인 후 확인 처리(실패는 건너뛰고 사유만 모음) → 3. 영향받은 현장에 브로드캐스트
    @Transactional
    public AlertBulkActionRes bulkConfirmAlerts(AlertBulkActionReq req) {
        UserPrincipal actor = getCurrentUser();
        List<Long> alertIds = req == null ? null : req.getAlertIds();
        validateAlertIds(alertIds);
        if (alertIds == null || alertIds.isEmpty()) {
            throw new BusinessException(AlertErrorCode.INVALID_ALERT_ID);
        }

        int successCount = 0;
        List<String> failureReasons = new ArrayList<>();
        Set<Long> siteIdsToNotify = new LinkedHashSet<>();

        for (Long alertId : alertIds) {
            try {
                Alert alert = findAccessibleAlert(actor, alertId);
                validateCanConfirm(alert);
                int updatedRows = alertMapper.confirmAlert(alertId, actor.getUserId());
                if (updatedRows == 0) {
                    throw new BusinessException(AlertErrorCode.ALERT_CANNOT_CONFIRM);
                }
                successCount++;
                Long siteId = alertMapper.findSiteIdByPanelId(alert.getPanelId());
                if (siteId != null) {
                    siteIdsToNotify.add(siteId);
                }
            } catch (BusinessException e) {
                failureReasons.add(e.getMessage());
            }
        }

        alertNotificationPublisher.publishBulkStatusChanged(siteIdsToNotify);
        return new AlertBulkActionRes(successCount, alertIds.size() - successCount, dedupeReasons(failureReasons));
    }

    // 경보 일괄 조치완료 처리 — 확인 처리와 동일한 구조. 비고는 PC 다중선택 화면과 동일하게 개별 입력을 받지 않는다.
    @Transactional
    public AlertBulkActionRes bulkResolveAlerts(AlertBulkActionReq req) {
        UserPrincipal actor = getCurrentUser();
        List<Long> alertIds = req == null ? null : req.getAlertIds();
        validateAlertIds(alertIds);
        if (alertIds == null || alertIds.isEmpty()) {
            throw new BusinessException(AlertErrorCode.INVALID_ALERT_ID);
        }

        int successCount = 0;
        List<String> failureReasons = new ArrayList<>();
        Set<Long> siteIdsToNotify = new LinkedHashSet<>();

        for (Long alertId : alertIds) {
            try {
                Alert alert = findAccessibleAlert(actor, alertId);
                validateCanResolve(alert);
                int updatedRows = alertMapper.resolveAlert(alertId, actor.getUserId(), null);
                if (updatedRows == 0) {
                    throw new BusinessException(AlertErrorCode.ALERT_NOT_CONFIRMED);
                }
                successCount++;
                Long siteId = alertMapper.findSiteIdByPanelId(alert.getPanelId());
                if (siteId != null) {
                    siteIdsToNotify.add(siteId);
                }
            } catch (BusinessException e) {
                failureReasons.add(e.getMessage());
            }
        }

        alertNotificationPublisher.publishBulkStatusChanged(siteIdsToNotify);
        return new AlertBulkActionRes(successCount, alertIds.size() - successCount, dedupeReasons(failureReasons));
    }

    // 실패 사유 중복 제거(같은 사유가 여러 건이면 한 번만 노출)
    private List<String> dedupeReasons(List<String> reasons) {
        return reasons.stream().distinct().toList();
    }

    // 현재 사용자가 접근할 수 있는 경보만 조회
    private Alert findAccessibleAlert(UserPrincipal actor, Long alertId) {
        if (alertId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());
        Alert alert = alertMapper.findAccessibleAlertById(actor.getUserId(), superAdmin, alertId);
        if (alert == null) {
            throw new BusinessException(AlertErrorCode.ALERT_NOT_FOUND);
        }
        return alert;
    }

    // 미확인 경보만 확인 처리 가능
    private void validateCanConfirm(Alert alert) {
        if (alert.getStatus() != AlertStatus.UNCONFIRMED) {
            throw new BusinessException(AlertErrorCode.ALERT_CANNOT_CONFIRM);
        }
    }

    // 확인된 경보만 조치완료 처리 가능
    private void validateCanResolve(Alert alert) {
        if (alert.getStatus() != AlertStatus.CONFIRMED) {
            throw new BusinessException(AlertErrorCode.ALERT_NOT_CONFIRMED);
        }
    }

    // 비고는 선택값. 공백만 입력하면 저장하지 않음
    private String normalizeResolutionNote(AlertResolveReq req) {
        if (req == null || req.getResolutionNote() == null) {
            return null;
        }
        String resolutionNote = req.getResolutionNote().trim();
        return resolutionNote.isEmpty() ? null : resolutionNote;
    }

    // null 요청도 기본 목록 조회로 처리
    private AlertListReq normalizeReq(AlertListReq req) {
        return req == null ? new AlertListReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(AlertListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(AlertListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(AlertListReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    // 선택 출력 ID는 양수만 허용
    private void validateAlertIds(List<Long> alertIds) {
        if (alertIds == null) {
            return;
        }
        boolean invalid = alertIds.stream().anyMatch(alertId -> alertId == null || alertId < 1);
        if (invalid) {
            throw new BusinessException(AlertErrorCode.INVALID_ALERT_ID);
        }
    }

    // 엑셀 다운로드는 관리자 화면 기능이므로 GENERAL은 제외
    private void validateAdminOrSuperAdmin(UserPrincipal actor) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole()) || UserRole.ADMIN.name().equals(actor.getRole())) {
            return;
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(AlertListReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(AlertListReq req) {
        return req.getTo() == null ? null : req.getTo().plusDays(1).atStartOfDay();
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }
}
