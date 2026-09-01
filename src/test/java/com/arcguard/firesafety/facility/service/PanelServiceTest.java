package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.PanelCreateReq;
import com.arcguard.firesafety.facility.dto.res.PanelCreateRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.facility.model.Site;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanelServiceTest {

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private CircuitService circuitService;

    private PanelService panelService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        panelService = new PanelService(panelMapper, siteMapper, circuitService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회로 개수가 1~10 범위를 벗어나면 등록할 수 없다")
    void invalidCircuitCountFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        PanelCreateReq req = createReq(11, null, null);

        // when & then
        assertThatThrownBy(() -> panelService.createPanel(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.INVALID_CIRCUIT_COUNT));
    }

    @Test
    @DisplayName("이미 등록된 장비 시리얼이면 409를 반환한다")
    void duplicatedDeviceSerialFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.existsPanelByDeviceSerial("DUP-001")).thenReturn(true);

        PanelCreateReq base = createReq(3, null, null);
        PanelCreateReq req = new PanelCreateReq(base.getName(), "DUP-001", base.getMNo(), null, 3, null, null, null, null, null, null);

        // when & then
        assertThatThrownBy(() -> panelService.createPanel(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.DUPLICATED_DEVICE_SERIAL));
    }

    @Test
    @DisplayName("이미 등록된 장비번호면 409를 반환한다")
    void duplicatedMNoFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.existsPanelByDeviceSerial(any())).thenReturn(false);
        when(panelMapper.existsPanelByMNo("00099")).thenReturn(true);

        PanelCreateReq req = createReq(3, null, null);

        // when & then
        assertThatThrownBy(() -> panelService.createPanel(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.DUPLICATED_M_NO));
    }

    @Test
    @DisplayName("FAC-003: ADMIN이 담당하지 않은 현장에는 분전반을 등록할 수 없다")
    void adminCannotCreatePanelOutsideAssignedSite() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(false);

        PanelCreateReq req = createReq(3, null, null);

        // when & then
        assertThatThrownBy(() -> panelService.createPanel(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("GENERAL은 분전반을 등록할 수 없다")
    void generalCannotCreatePanel() {
        // given
        loginAs(3L, UserRole.GENERAL);
        PanelCreateReq req = createReq(3, null, null);

        // when & then
        assertThatThrownBy(() -> panelService.createPanel(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("임계치 미입력 시 누설전류/온도/습도/과전류/가스/불꽃 모두 기본값이 채워진다")
    void defaultThresholdsAppliedWhenMissing() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.existsPanelByDeviceSerial(any())).thenReturn(false);
        when(panelMapper.findActivePanelById(any())).thenReturn(savedPanel());

        PanelCreateReq req = createReq(3, null, null);

        // when
        panelService.createPanel(1L, req);

        // then
        ArgumentCaptor<Panel> captor = ArgumentCaptor.forClass(Panel.class);
        verify(panelMapper).insertPanel(captor.capture());
        Panel inserted = captor.getValue();
        assertThat(inserted.getLeakMaThreshold()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(inserted.getTempThreshold()).isEqualByComparingTo(BigDecimal.valueOf(80.0));
        assertThat(inserted.getHumidityThreshold()).isEqualByComparingTo(BigDecimal.valueOf(80.0));
        assertThat(inserted.getOvercurrentThreshold()).isEqualByComparingTo(BigDecimal.valueOf(30.0));
        assertThat(inserted.getGasThreshold()).isEqualTo(5000);
        assertThat(inserted.getFireThreshold()).isEqualTo(5000);
    }

    @Test
    @DisplayName("가스/불꽃 임계치를 직접 입력하면 그 값을 그대로 저장한다")
    void customGasFireThresholdKept() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.existsPanelByDeviceSerial(any())).thenReturn(false);
        when(panelMapper.findActivePanelById(any())).thenReturn(savedPanel());

        PanelCreateReq req = createReq(3, 2000, 3000);

        // when
        panelService.createPanel(1L, req);

        // then
        ArgumentCaptor<Panel> captor = ArgumentCaptor.forClass(Panel.class);
        verify(panelMapper).insertPanel(captor.capture());
        assertThat(captor.getValue().getGasThreshold()).isEqualTo(2000);
        assertThat(captor.getValue().getFireThreshold()).isEqualTo(3000);
    }

    @Test
    @DisplayName("등록 직후 분전반 상태는 통신 이력이 없으므로 OFFLINE으로 시작한다")
    void newPanelStartsOffline() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.existsPanelByDeviceSerial(any())).thenReturn(false);
        when(panelMapper.findActivePanelById(any())).thenReturn(savedPanel());

        PanelCreateReq req = createReq(3, null, null);

        // when
        panelService.createPanel(1L, req);

        // then
        ArgumentCaptor<Panel> captor = ArgumentCaptor.forClass(Panel.class);
        verify(panelMapper).insertPanel(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PanelStatus.OFFLINE);
        assertThat(captor.getValue().getIsOnline()).isFalse();
    }

    @Test
    @DisplayName("정상 요청이면 분전반을 등록하고 감사 로그를 남긴다")
    void createPanelSuccess() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.existsPanelByDeviceSerial(any())).thenReturn(false);
        when(panelMapper.findActivePanelById(any())).thenReturn(savedPanel());

        PanelCreateReq req = createReq(3, null, null);

        // when
        PanelCreateRes result = panelService.createPanel(1L, req);

        // then
        assertThat(result.getName()).isEqualTo("분전반1");
        verify(siteMapper).insertFacilityAuditLog(any());
        verify(circuitService).createCircuitsForPanel(savedPanel().getPanelId(), savedPanel().getCircuitCount(), 1L);
    }

    @Test
    @DisplayName("분전반 삭제 시 소속 회로도 함께 소프트 삭제한다")
    void deletePanelCascadesToCircuits() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(panelMapper.findActivePanelById(1L)).thenReturn(savedPanel());
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(panelMapper.softDeletePanel(1L)).thenReturn(1);

        // when
        panelService.deletePanel(1L);

        // then
        verify(panelMapper).softDeletePanel(1L);
        verify(circuitService).deleteCircuitsForPanel(1L, 1L);
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private PanelCreateReq createReq(Integer circuitCount, Integer gasThreshold, Integer fireThreshold) {
        return new PanelCreateReq(
                "분전반1",
                "DEMO-SERIAL-001",
                "00099",
                null,
                circuitCount,
                null,
                null,
                null,
                null,
                gasThreshold,
                fireThreshold
        );
    }

    private Site site(Long siteId) {
        Site site = new Site();
        site.setSiteId(siteId);
        site.setName("아크타워1");
        return site;
    }

    private Panel savedPanel() {
        Panel panel = new Panel();
        panel.setPanelId(1L);
        panel.setSiteId(1L);
        panel.setName("분전반1");
        panel.setDeviceSerial("DEMO-SERIAL-001");
        panel.setMNo("00099");
        panel.setStatus(PanelStatus.NORMAL);
        panel.setCircuitCount(3);
        panel.setLeakMaThreshold(BigDecimal.valueOf(20.0));
        panel.setTempThreshold(BigDecimal.valueOf(80.0));
        panel.setHumidityThreshold(BigDecimal.valueOf(80.0));
        panel.setOvercurrentThreshold(BigDecimal.valueOf(30.0));
        panel.setGasThreshold(5000);
        panel.setFireThreshold(5000);
        return panel;
    }
}
