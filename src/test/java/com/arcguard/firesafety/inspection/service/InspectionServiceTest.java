package com.arcguard.firesafety.inspection.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.Site;
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
import com.arcguard.firesafety.inspection.model.InspectionResultType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @Mock
    private InspectionMapper inspectionMapper;

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private InspectionExcelService inspectionExcelService;

    private InspectionService inspectionService;

    @BeforeEach
    void setUp() {
        inspectionService = new InspectionService(inspectionMapper, panelMapper, siteMapper, inspectionExcelService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-014: ADMIN은 담당 현장 점검 항목 카탈로그에 항목을 등록할 수 있다")
    void adminCanCreateItem() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        InspectionItemCreateReq req = new InspectionItemCreateReq();
        req.setItemName("누전차단기 동작 확인");

        // when
        InspectionItemCreateRes res = inspectionService.createItem(3L, req);

        // then
        assertThat(res).isNotNull();
        verify(inspectionMapper).insertInspectionItem(org.mockito.ArgumentMatchers.argThat(item ->
                item.getSiteId().equals(3L) && item.getItemName().equals("누전차단기 동작 확인")));
    }

    @Test
    @DisplayName("FAC-014: GENERAL은 점검 항목을 등록할 수 없다")
    void generalCannotCreateItem() {
        // given
        loginAs(3L, UserRole.GENERAL);
        InspectionItemCreateReq req = new InspectionItemCreateReq();
        req.setItemName("누전차단기 동작 확인");

        // when & then
        assertThatThrownBy(() -> inspectionService.createItem(3L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("FAC-014: 항목명이 없으면 등록할 수 없다")
    void createItemFailsWithoutItemName() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> inspectionService.createItem(3L, new InspectionItemCreateReq()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(InspectionErrorCode.ITEM_NAME_REQUIRED));
    }

    @Test
    @DisplayName("현장 점검 항목 카탈로그를 조회할 수 있다")
    void canGetSiteItems() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.findInspectionItemsBySiteId(3L)).thenReturn(List.of(itemRes()));

        // when
        List<InspectionItemRes> items = inspectionService.getSiteItems(3L);

        // then
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("ADMIN은 점검 항목을 수정할 수 있다")
    void adminCanUpdateItem() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItemInSite(100L, 3L)).thenReturn(true);
        InspectionItemCreateReq req = new InspectionItemCreateReq();
        req.setItemName("누전차단기 동작 확인(수정)");

        // when
        inspectionService.updateItem(3L, 100L, req);

        // then
        verify(inspectionMapper).updateInspectionItem(org.mockito.ArgumentMatchers.argThat(item ->
                item.getItemId().equals(100L) && item.getItemName().equals("누전차단기 동작 확인(수정)")));
    }

    @Test
    @DisplayName("GENERAL은 점검 항목을 수정할 수 없다")
    void generalCannotUpdateItem() {
        // given
        loginAs(3L, UserRole.GENERAL);
        InspectionItemCreateReq req = new InspectionItemCreateReq();
        req.setItemName("누전차단기 동작 확인(수정)");

        // when & then
        assertThatThrownBy(() -> inspectionService.updateItem(3L, 100L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("ADMIN은 사용 중이 아닌 점검 항목을 삭제할 수 있다")
    void adminCanDeleteUnusedItem() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItemInSite(100L, 3L)).thenReturn(true);
        when(inspectionMapper.existsPanelInspectionItemByItemId(100L)).thenReturn(false);
        when(inspectionMapper.existsInspectionResultItemByItemId(100L)).thenReturn(false);

        // when
        inspectionService.deleteItem(3L, 100L);

        // then
        verify(inspectionMapper).deleteInspectionItem(100L);
    }

    @Test
    @DisplayName("이미 분전반에 적용된 점검 항목은 삭제할 수 없다")
    void deleteItemFailsWhenAppliedToPanel() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItemInSite(100L, 3L)).thenReturn(true);
        when(inspectionMapper.existsPanelInspectionItemByItemId(100L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> inspectionService.deleteItem(3L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(InspectionErrorCode.ITEM_IN_USE));
    }

    @Test
    @DisplayName("ADMIN은 분전반에 카탈로그 항목을 일괄 적용(전체교체)할 수 있다")
    void adminCanApplyItems() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItemInSite(100L, 3L)).thenReturn(true);
        InspectionItemApplyReq req = new InspectionItemApplyReq();
        req.setItemIds(List.of(100L));

        // when
        inspectionService.applyItems(10L, req);

        // then
        verify(inspectionMapper).deletePanelInspectionItems(10L);
        verify(inspectionMapper).insertPanelInspectionItems(10L, List.of(100L));
    }

    @Test
    @DisplayName("GENERAL도 담당 현장 분전반에 점검 항목을 적용할 수 있다(카탈로그 등록과 달리 실무 작업으로 취급)")
    void generalCanApplyItems() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItemInSite(100L, 3L)).thenReturn(true);
        InspectionItemApplyReq req = new InspectionItemApplyReq();
        req.setItemIds(List.of(100L));

        // when
        inspectionService.applyItems(10L, req);

        // then
        verify(inspectionMapper).deletePanelInspectionItems(10L);
        verify(inspectionMapper).insertPanelInspectionItems(10L, List.of(100L));
    }

    @Test
    @DisplayName("담당 현장이 아니면 점검 항목을 적용할 수 없다")
    void applyItemsFailsWhenSiteNotAssigned() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(false);
        InspectionItemApplyReq req = new InspectionItemApplyReq();
        req.setItemIds(List.of(100L));

        // when & then
        assertThatThrownBy(() -> inspectionService.applyItems(10L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("다른 현장 소속 항목은 분전반에 적용할 수 없다")
    void applyItemsFailsWhenItemNotInSite() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItemInSite(999L, 3L)).thenReturn(false);
        InspectionItemApplyReq req = new InspectionItemApplyReq();
        req.setItemIds(List.of(999L));

        // when & then
        assertThatThrownBy(() -> inspectionService.applyItems(10L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(InspectionErrorCode.ITEM_NOT_IN_SITE));
    }

    @Test
    @DisplayName("FAC-014: ADMIN은 담당 현장 분전반의 점검 항목 목록을 조회할 수 있다")
    void adminCanGetItems() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(inspectionMapper.findInspectionItemsByPanelId(10L)).thenReturn(List.of(itemRes()));

        // when
        List<InspectionItemRes> items = inspectionService.getItems(10L);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getItemName()).isEqualTo("누전차단기 동작 확인");
    }

    @Test
    @DisplayName("FAC-014: GENERAL은 담당 현장 분전반의 점검 항목 목록을 조회할 수 있다")
    void generalCanGetItems() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(true);
        when(inspectionMapper.findInspectionItemsByPanelId(10L)).thenReturn(List.of(itemRes()));

        // when
        List<InspectionItemRes> items = inspectionService.getItems(10L);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getItemName()).isEqualTo("누전차단기 동작 확인");
    }

    @Test
    @DisplayName("FAC-014: 분전반이 없으면 점검 항목 목록을 조회할 수 없다")
    void getItemsFailsWhenPanelNotFound() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(panelMapper.findActivePanelById(10L)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> inspectionService.getItems(10L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.PANEL_NOT_FOUND));
    }

    @Test
    @DisplayName("FAC-014: 점검 체크리스트를 저장하면 항목별 결과가 함께 저장된다")
    void saveChecklistSavesResultAndItems() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItem(100L, 10L)).thenReturn(true);

        InspectionResultItemReq resultItemReq = new InspectionResultItemReq();
        resultItemReq.setItemId(100L);
        resultItemReq.setResult(InspectionResultType.NORMAL);
        InspectionSaveReq req = new InspectionSaveReq();
        req.setResults(List.of(resultItemReq));
        req.setNote("  이상 없음  ");

        // when
        InspectionSaveRes res = inspectionService.saveChecklist(10L, req);

        // then
        assertThat(res).isNotNull();
        verify(inspectionMapper).insertInspectionResult(org.mockito.ArgumentMatchers.argThat(result ->
                result.getPanelId().equals(10L) && result.getInspectorId().equals(3L) && "이상 없음".equals(result.getNote())));
        verify(inspectionMapper).insertInspectionResultItem(org.mockito.ArgumentMatchers.argThat(item ->
                item.getItemId().equals(100L) && item.getResult() == InspectionResultType.NORMAL));
    }

    @Test
    @DisplayName("FAC-014: 결과 목록이 비어있으면 체크리스트를 저장할 수 없다")
    void saveChecklistFailsWithEmptyResults() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> inspectionService.saveChecklist(10L, new InspectionSaveReq()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(InspectionErrorCode.RESULTS_REQUIRED));
    }

    @Test
    @DisplayName("FAC-014: 다른 분전반 소속 항목ID로는 체크리스트를 저장할 수 없다")
    void saveChecklistFailsWhenItemBelongsToAnotherPanel() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(true);
        when(inspectionMapper.existsInspectionItem(999L, 10L)).thenReturn(false);

        InspectionResultItemReq resultItemReq = new InspectionResultItemReq();
        resultItemReq.setItemId(999L);
        resultItemReq.setResult(InspectionResultType.NORMAL);
        InspectionSaveReq req = new InspectionSaveReq();
        req.setResults(List.of(resultItemReq));

        // when & then
        assertThatThrownBy(() -> inspectionService.saveChecklist(10L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(InspectionErrorCode.ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("FAC-015: 점검 이력을 조회하면 이력 건별로 항목 결과가 함께 조립된다")
    void getHistoryAssemblesResultItems() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(inspectionMapper.findInspectionHistory(10L, null, null, 20, 0))
                .thenReturn(List.of(historyRes()));
        when(inspectionMapper.findResultItemsByInspectionId(1L))
                .thenReturn(List.of());
        when(inspectionMapper.countInspectionHistory(10L, null, null)).thenReturn(1L);

        // when
        InspectionHistoryPageRes res = inspectionService.getHistory(10L, null);

        // then
        assertThat(res.getTotalElements()).isEqualTo(1L);
        verify(inspectionMapper).findResultItemsByInspectionId(1L);
    }

    @Test
    @DisplayName("FAC-015: GENERAL은 담당 현장 분전반의 점검 이력을 엑셀로 다운로드할 수 있다")
    void generalCanExportHistory() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.existsActiveSiteAssignment(3L, 3L)).thenReturn(true);
        when(inspectionMapper.findInspectionExportRows(10L, null, null)).thenReturn(List.of(exportRowRes()));
        when(inspectionExcelService.createInspectionHistoryExcel(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new byte[]{1, 2, 3});

        // when
        byte[] excel = inspectionService.exportHistory(10L, null);

        // then
        assertThat(excel).containsExactly(1, 2, 3);
        verify(inspectionMapper).findInspectionExportRows(10L, null, null);
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Panel panel() {
        Panel panel = new Panel();
        panel.setPanelId(10L);
        panel.setSiteId(3L);
        return panel;
    }

    private Site site() {
        Site site = new Site();
        site.setSiteId(3L);
        return site;
    }

    private InspectionItemRes itemRes() {
        InspectionItemRes res = new InspectionItemRes();
        res.setItemId(100L);
        res.setSiteId(3L);
        res.setItemName("누전차단기 동작 확인");
        return res;
    }

    private InspectionHistoryRes historyRes() {
        InspectionHistoryRes res = new InspectionHistoryRes();
        res.setInspectionId(1L);
        res.setInspectorName("홍길동");
        return res;
    }

    private InspectionExportRowRes exportRowRes() {
        InspectionExportRowRes res = new InspectionExportRowRes();
        res.setInspectionId(1L);
        res.setSiteName("아크타워1호점");
        res.setPanelName("2층 분전반");
        res.setItemId(100L);
        res.setItemName("누전차단기 동작 확인");
        res.setResult(InspectionResultType.NORMAL);
        return res;
    }
}
