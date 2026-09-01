package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.CircuitCreateReq;
import com.arcguard.firesafety.facility.dto.req.CircuitUpdateReq;
import com.arcguard.firesafety.facility.dto.res.CircuitCreateRes;
import com.arcguard.firesafety.facility.dto.res.CircuitDetailRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.CircuitMapper;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.Site;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitServiceTest {

    @Mock
    private CircuitMapper circuitMapper;

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private SiteMapper siteMapper;

    private CircuitService circuitService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        circuitService = new CircuitService(circuitMapper, panelMapper, siteMapper, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-001: 회로 번호가 분전반 circuit_count 범위를 초과하면 등록할 수 없다")
    void channelNoExceedsCircuitCount() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 3);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);

        CircuitCreateReq req = new CircuitCreateReq(4, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.INVALID_CHANNEL_NO));
    }

    @Test
    @DisplayName("FAC-001: 회로 번호가 물리 최대값 10을 초과하면 등록할 수 없다")
    void channelNoExceedsPhysicalMax() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);

        CircuitCreateReq req = new CircuitCreateReq(11, null);

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.INVALID_CHANNEL_NO));
    }

    @Test
    @DisplayName("FAC-002: 같은 분전반에 동일한 채널 번호를 중복 등록할 수 없다")
    void duplicatedChannelNo() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);
        when(circuitMapper.findCircuitByPanelIdAndChannelNo(1L, 1)).thenReturn(savedCircuit());

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.DUPLICATED_CHANNEL_NO));
    }

    @Test
    @DisplayName("FAC-002: 삭제됐던 채널번호로 재등록하면 새로 만들지 않고 재활성화한다")
    void reactivateDeletedChannelNo() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);

        com.arcguard.firesafety.facility.model.Circuit deletedCircuit = savedCircuit();
        deletedCircuit.setDeletedAt(java.time.LocalDateTime.now());
        when(circuitMapper.findCircuitByPanelIdAndChannelNo(1L, 1)).thenReturn(deletedCircuit);
        when(circuitMapper.findActiveCircuitById(1L)).thenReturn(savedCircuit());

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when
        CircuitCreateRes result = circuitService.createCircuit(1L, req);

        // then
        assertThat(result.getCircuitId()).isEqualTo(1L);
        verify(circuitMapper).reactivateCircuit(1L, "조명");
        verify(circuitMapper, org.mockito.Mockito.never()).insertCircuit(any());
    }

    @Test
    @DisplayName("FAC-003: ADMIN이 담당하지 않은 현장의 분전반에는 회로를 등록할 수 없다")
    void adminCannotCreateCircuitOutsideAssignedSite() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(false);

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("GENERAL은 회로를 등록할 수 없다")
    void generalCannotCreateCircuit() {
        // given
        loginAs(1L, UserRole.GENERAL);
        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("정상 요청이면 회로를 등록하고 감사 로그를 남긴다")
    void createCircuitSuccess() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);
        when(circuitMapper.findCircuitByPanelIdAndChannelNo(1L, 1)).thenReturn(null);
        when(circuitMapper.findActiveCircuitById(any())).thenReturn(savedCircuit());

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when
        CircuitCreateRes result = circuitService.createCircuit(1L, req);

        // then
        assertThat(result.getChannelNo()).isEqualTo(1);
        assertThat(result.getPanelId()).isEqualTo(1L);
        verify(siteMapper).insertFacilityAuditLog(any());
    }

    @Test
    @DisplayName("분전반 등록 시 채널 1~circuitCount까지 loadType 없이 회로를 일괄 생성한다")
    void createCircuitsForPanelBulkCreates() {
        // given
        when(circuitMapper.findActiveCircuitById(any())).thenReturn(savedCircuit());

        // when
        circuitService.createCircuitsForPanel(1L, 3, 1L);

        // then
        verify(circuitMapper, org.mockito.Mockito.times(3)).insertCircuit(any());
        verify(siteMapper, org.mockito.Mockito.times(3)).insertFacilityAuditLog(any());
    }

    @Test
    @DisplayName("분전반 삭제 시 소속 활성 회로를 전부 소프트 삭제하고 건별로 감사 로그를 남긴다")
    void deleteCircuitsForPanelSoftDeletesAll() {
        // given
        com.arcguard.firesafety.facility.model.Circuit c1 = savedCircuit();
        com.arcguard.firesafety.facility.model.Circuit c2 = savedCircuit();
        c2.setCircuitId(2L);
        c2.setChannelNo(2);
        when(circuitMapper.findActiveCircuitsByPanelId(1L)).thenReturn(java.util.List.of(c1, c2));

        // when
        circuitService.deleteCircuitsForPanel(1L, 9L);

        // then
        verify(circuitMapper).softDeleteCircuit(1L);
        verify(circuitMapper).softDeleteCircuit(2L);
        verify(siteMapper, org.mockito.Mockito.times(2)).insertFacilityAuditLog(any());
    }

    @Test
    @DisplayName("회로 부하종류를 수정하면 감사 로그를 남긴다")
    void updateCircuitSuccess() {
        // given
        loginAs(1L, UserRole.ADMIN);
        when(circuitMapper.findActiveCircuitById(1L)).thenReturn(savedCircuit());
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel(1L, 1L, 10));
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);

        com.arcguard.firesafety.facility.model.Circuit updated = savedCircuit();
        updated.setLoadType("에어컨");
        when(circuitMapper.findActiveCircuitById(1L)).thenReturn(savedCircuit(), updated);

        // when
        CircuitDetailRes result = circuitService.updateCircuit(1L, new CircuitUpdateReq("에어컨"));

        // then
        assertThat(result.getLoadType()).isEqualTo("에어컨");
        verify(circuitMapper).updateCircuitLoadType(1L, "에어컨");
        verify(siteMapper).insertFacilityAuditLog(any());
    }

    @Test
    @DisplayName("회로 부하종류가 50자를 초과하면 수정할 수 없다")
    void updateCircuitLoadTypeTooLongFails() {
        // given
        loginAs(1L, UserRole.ADMIN);
        String tooLong = "a".repeat(51);

        // when & then
        assertThatThrownBy(() -> circuitService.updateCircuit(1L, new CircuitUpdateReq(tooLong)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.LOAD_TYPE_TOO_LONG));
    }

    @Test
    @DisplayName("GENERAL은 회로 부하종류를 수정할 수 없다")
    void generalCannotUpdateCircuit() {
        // given
        loginAs(1L, UserRole.GENERAL);

        // when & then
        assertThatThrownBy(() -> circuitService.updateCircuit(1L, new CircuitUpdateReq("조명")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Panel panel(Long panelId, Long siteId, int circuitCount) {
        Panel panel = new Panel();
        panel.setPanelId(panelId);
        panel.setSiteId(siteId);
        panel.setCircuitCount(circuitCount);
        return panel;
    }

    private Site site(Long siteId) {
        Site site = new Site();
        site.setSiteId(siteId);
        site.setName("아크타워1");
        return site;
    }

    private com.arcguard.firesafety.facility.model.Circuit savedCircuit() {
        com.arcguard.firesafety.facility.model.Circuit circuit = new com.arcguard.firesafety.facility.model.Circuit();
        circuit.setCircuitId(1L);
        circuit.setPanelId(1L);
        circuit.setChannelNo(1);
        circuit.setLoadType("조명");
        return circuit;
    }
}
