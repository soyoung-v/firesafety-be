package com.arcguard.firesafety.inspection.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.Site;
import com.arcguard.firesafety.inspection.dto.req.InspectionHistoryListReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionItemApplyReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionItemCreateReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionResultItemReq;
import com.arcguard.firesafety.inspection.dto.req.InspectionSaveReq;
import com.arcguard.firesafety.inspection.dto.res.InspectionExportRowRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionHistoryPageRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionHistoryRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionItemCreateRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionItemRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionSaveRes;
import com.arcguard.firesafety.inspection.exception.InspectionErrorCode;
import com.arcguard.firesafety.inspection.mapper.InspectionMapper;
import com.arcguard.firesafety.inspection.model.InspectionItem;
import com.arcguard.firesafety.inspection.model.InspectionResult;
import com.arcguard.firesafety.inspection.model.InspectionResultItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final InspectionMapper inspectionMapper;
    private final PanelMapper panelMapper;
    private final SiteMapper siteMapper;
    private final InspectionExcelService inspectionExcelService;

    // 점검 항목 등록 (REQ-511, ADMIN 이상) — 분전반이 아니라 현장 카탈로그에 등록한다
    @Transactional
    public InspectionItemCreateRes createItem(Long siteId, InspectionItemCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateAdminOrSuperAdmin(actor);
        findActiveSite(siteId);
        validateSiteAccess(actor, siteId);
        validateItemName(req);

        InspectionItem item = new InspectionItem();
        item.setSiteId(siteId);
        item.setItemName(req.getItemName().trim());
        item.setDescription(normalizeDescription(req.getDescription()));
        inspectionMapper.insertInspectionItem(item);

        return new InspectionItemCreateRes(item.getItemId());
    }

    // 점검 항목 수정 (ADMIN 이상)
    @Transactional
    public void updateItem(Long siteId, Long itemId, InspectionItemCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateAdminOrSuperAdmin(actor);
        findActiveSite(siteId);
        validateSiteAccess(actor, siteId);
        validateItemName(req);
        if (!inspectionMapper.existsInspectionItemInSite(itemId, siteId)) {
            throw new BusinessException(InspectionErrorCode.ITEM_NOT_FOUND);
        }

        InspectionItem item = new InspectionItem();
        item.setItemId(itemId);
        item.setItemName(req.getItemName().trim());
        item.setDescription(normalizeDescription(req.getDescription()));
        inspectionMapper.updateInspectionItem(item);
    }

    // 점검 항목 삭제 (ADMIN 이상) — 이미 분전반에 적용됐거나 점검 결과에 쓰인 적 있으면 삭제 불가(하드 삭제라 참조 무결성 대신 사전 확인으로 막는다)
    @Transactional
    public void deleteItem(Long siteId, Long itemId) {
        UserPrincipal actor = getCurrentUser();
        validateAdminOrSuperAdmin(actor);
        findActiveSite(siteId);
        validateSiteAccess(actor, siteId);
        if (!inspectionMapper.existsInspectionItemInSite(itemId, siteId)) {
            throw new BusinessException(InspectionErrorCode.ITEM_NOT_FOUND);
        }
        if (inspectionMapper.existsPanelInspectionItemByItemId(itemId) || inspectionMapper.existsInspectionResultItemByItemId(itemId)) {
            throw new BusinessException(InspectionErrorCode.ITEM_IN_USE);
        }
        inspectionMapper.deleteInspectionItem(itemId);
    }

    // 현장의 점검 항목 카탈로그 전체 조회 (GENERAL 이상) — 분전반에 적용할 항목을 고를 때 후보 목록으로 쓴다
    @Transactional(readOnly = true)
    public List<InspectionItemRes> getSiteItems(Long siteId) {
        UserPrincipal actor = getCurrentUser();
        findActiveSite(siteId);
        validateSiteAccess(actor, siteId);

        return inspectionMapper.findInspectionItemsBySiteId(siteId);
    }

    // 분전반에 적용된 점검 항목 목록 조회 (REQ-511, GENERAL 이상)
    @Transactional(readOnly = true)
    public List<InspectionItemRes> getItems(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        return inspectionMapper.findInspectionItemsByPanelId(panelId);
    }

    // 분전반에 점검 항목 일괄 적용 (GENERAL 이상) — 카탈로그(항목 정의)는 ADMIN 전용이지만, 이미 있는 항목 중
    // 이 분전반에서 무엇을 점검할지 고르는 건 점검결과 저장(saveChecklist)과 같은 등급의 실무 작업으로 본다.
    // 현재 적용 상태를 요청 목록으로 전체교체한다.
    @Transactional
    public void applyItems(Long panelId, InspectionItemApplyReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        List<Long> itemIds = req != null && req.getItemIds() != null ? req.getItemIds() : List.of();
        for (Long itemId : itemIds) {
            if (!inspectionMapper.existsInspectionItemInSite(itemId, panel.getSiteId())) {
                throw new BusinessException(InspectionErrorCode.ITEM_NOT_IN_SITE);
            }
        }

        inspectionMapper.deletePanelInspectionItems(panelId);
        if (!itemIds.isEmpty()) {
            inspectionMapper.insertPanelInspectionItems(panelId, itemIds);
        }
    }

    // 점검 체크리스트 저장 (REQ-511)
    // 1. 권한 확인 → 2. 결과 유효성 확인(항목이 이 분전반 소속인지까지) → 3. 점검 실행 저장 → 4. 항목별 결과 저장
    @Transactional
    public InspectionSaveRes saveChecklist(Long panelId, InspectionSaveReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        validateResults(panelId, req);

        InspectionResult inspectionResult = new InspectionResult();
        inspectionResult.setPanelId(panelId);
        inspectionResult.setInspectedAt(req.getInspectedAt() != null ? req.getInspectedAt() : LocalDateTime.now());
        inspectionResult.setInspectorId(actor.getUserId());
        inspectionResult.setNote(normalizeDescription(req.getNote()));
        inspectionMapper.insertInspectionResult(inspectionResult);

        for (InspectionResultItemReq resultItemReq : req.getResults()) {
            InspectionResultItem resultItem = new InspectionResultItem();
            resultItem.setInspectionId(inspectionResult.getInspectionId());
            resultItem.setItemId(resultItemReq.getItemId());
            resultItem.setResult(resultItemReq.getResult());
            inspectionMapper.insertInspectionResultItem(resultItem);
        }

        return new InspectionSaveRes(inspectionResult.getInspectionId());
    }

    // 점검 이력 조회 (REQ-512)
    // 이력 목록을 먼저 페이지 단위로 조회한 뒤, 건별로 항목 결과를 붙여서 조립한다
    @Transactional(readOnly = true)
    public InspectionHistoryPageRes getHistory(Long panelId, InspectionHistoryListReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        InspectionHistoryListReq searchReq = normalizeHistoryReq(req);
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;
        validateDateRange(searchReq);
        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);

        List<InspectionHistoryRes> content = inspectionMapper.findInspectionHistory(panelId, fromAt, toAt, size, offset);
        content.forEach(history -> history.setResults(inspectionMapper.findResultItemsByInspectionId(history.getInspectionId())));
        long totalElements = inspectionMapper.countInspectionHistory(panelId, fromAt, toAt);

        return new InspectionHistoryPageRes(content, totalElements);
    }

    // 점검 이력 엑셀 다운로드 (REQ-512)
    // 1. 현재 사용자 확인 → 2. 현장 접근 확인 → 3. 기간 검증 → 4. 항목별 row 조회 → 5. xlsx 생성
    @Transactional(readOnly = true)
    public byte[] exportHistory(Long panelId, InspectionHistoryListReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        InspectionHistoryListReq searchReq = normalizeHistoryReq(req);
        validateDateRange(searchReq);
        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);

        List<InspectionExportRowRes> rows = inspectionMapper.findInspectionExportRows(panelId, fromAt, toAt);
        return inspectionExcelService.createInspectionHistoryExcel(rows, searchReq);
    }

    // 활성 분전반 조회
    private Panel findActivePanel(Long panelId) {
        Panel panel = panelMapper.findActivePanelById(panelId);
        if (panel == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
    }

    // 활성 현장 조회
    private Site findActiveSite(Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }
        return site;
    }

    // 현장 접근 권한 확인 - SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장만
    private void validateSiteAccess(UserPrincipal actor, Long siteId) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // 점검 항목 등록은 ADMIN 이상만 가능
    private void validateAdminOrSuperAdmin(UserPrincipal actor) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole()) || UserRole.ADMIN.name().equals(actor.getRole())) {
            return;
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 항목명 필수 입력 검증
    private void validateItemName(InspectionItemCreateReq req) {
        if (req.getItemName() == null || req.getItemName().isBlank()) {
            throw new BusinessException(InspectionErrorCode.ITEM_NAME_REQUIRED);
        }
    }

    // 결과 목록이 비어있지 않고, 항목들이 전부 이 분전반 소속인지 확인
    private void validateResults(Long panelId, InspectionSaveReq req) {
        if (req.getResults() == null || req.getResults().isEmpty()) {
            throw new BusinessException(InspectionErrorCode.RESULTS_REQUIRED);
        }
        for (InspectionResultItemReq resultItemReq : req.getResults()) {
            if (!inspectionMapper.existsInspectionItem(resultItemReq.getItemId(), panelId)) {
                throw new BusinessException(InspectionErrorCode.ITEM_NOT_FOUND);
            }
        }
    }

    // 공백만 입력하면 저장하지 않음
    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // null 요청도 기본 목록 조회로 처리
    private InspectionHistoryListReq normalizeHistoryReq(InspectionHistoryListReq req) {
        return req == null ? new InspectionHistoryListReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(InspectionHistoryListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(InspectionHistoryListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(InspectionHistoryListReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(InspectionHistoryListReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(InspectionHistoryListReq req) {
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
