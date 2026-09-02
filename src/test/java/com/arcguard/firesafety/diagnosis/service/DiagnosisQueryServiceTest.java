package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.diagnosis.config.AiPredictionProperties;
import com.arcguard.firesafety.diagnosis.dto.req.DiagnosisResultListReq;
import com.arcguard.firesafety.diagnosis.dto.res.DiagnosisExplanationRes;
import com.arcguard.firesafety.diagnosis.dto.res.DiagnosisResultPageRes;
import com.arcguard.firesafety.diagnosis.dto.res.DiagnosisResultRes;
import com.arcguard.firesafety.diagnosis.dto.res.PanelDiagnosisRecentRes;
import com.arcguard.firesafety.diagnosis.dto.res.PanelDiagnosisSummaryRes;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.CircuitMapper;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Circuit;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisQueryServiceTest {

    @Mock
    private AiDiagnosisResultMapper aiDiagnosisResultMapper;

    @Mock
    private CircuitMapper circuitMapper;

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private AiPredictionService aiPredictionService;

    @Mock
    private AiDiagnosisExplanationService aiDiagnosisExplanationService;

    private DiagnosisQueryService diagnosisQueryService;

    @BeforeEach
    void setUp() {
        AiPredictionProperties aiPredictionProperties = new AiPredictionProperties();
        aiPredictionProperties.setMinSampleSize(30);
        diagnosisQueryService = new DiagnosisQueryService(
                aiDiagnosisResultMapper,
                circuitMapper,
                panelMapper,
                siteMapper,
                aiPredictionService,
                aiPredictionProperties,
                aiDiagnosisExplanationService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-017: SUPER_ADMIN은 회로 AI 진단결과를 조회할 수 있다")
    void superAdminCanGetDiagnosisResults() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(aiDiagnosisResultMapper.findDiagnosisResults(20L, 20, 0)).thenReturn(List.of(diagnosisResult()));
        when(aiDiagnosisResultMapper.countDiagnosisResults(20L)).thenReturn(1L);

        // when
        DiagnosisResultPageRes result = diagnosisQueryService.getDiagnosisResults(20L, new DiagnosisResultListReq());

        // then
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        verify(aiDiagnosisResultMapper).findDiagnosisResults(20L, 20, 0);
    }

    @Test
    @DisplayName("API-017: GENERAL은 담당 현장 회로의 AI 진단결과를 조회할 수 있다")
    void generalCanGetAssignedSiteDiagnosisResults() {
        // given
        loginAs(2L, UserRole.GENERAL);
        DiagnosisResultListReq req = new DiagnosisResultListReq();
        req.setPage(1);
        req.setSize(10);

        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(aiDiagnosisResultMapper.findDiagnosisResults(20L, 10, 10)).thenReturn(List.of());
        when(aiDiagnosisResultMapper.countDiagnosisResults(20L)).thenReturn(0L);

        // when
        DiagnosisResultPageRes result = diagnosisQueryService.getDiagnosisResults(20L, req);

        // then
        assertThat(result.getTotalElements()).isZero();
        verify(aiDiagnosisResultMapper).findDiagnosisResults(20L, 10, 10);
    }

    @Test
    @DisplayName("API-017: verdict=NORMAL이면 원본 proba를 (1-proba)로 바꿔서 확신도로 내려준다")
    void normalVerdictConfidenceIsInverted() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        DiagnosisResultRes normalResult = diagnosisResult();
        normalResult.setVerdict(com.arcguard.firesafety.diagnosis.model.Verdict.NORMAL);
        normalResult.setConfidence(0.0000217681f);
        when(aiDiagnosisResultMapper.findDiagnosisResults(20L, 20, 0)).thenReturn(List.of(normalResult));
        when(aiDiagnosisResultMapper.countDiagnosisResults(20L)).thenReturn(1L);

        // when
        DiagnosisResultPageRes result = diagnosisQueryService.getDiagnosisResults(20L, new DiagnosisResultListReq());

        // then
        assertThat(result.getContent().get(0).getConfidence()).isCloseTo(0.99998f, org.assertj.core.data.Offset.offset(0.0001f));
    }

    @Test
    @DisplayName("API-017: verdict=ARC이면 원본 proba를 그대로 확신도로 내려준다")
    void arcVerdictConfidenceIsUnchanged() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        DiagnosisResultRes arcResult = diagnosisResult();
        arcResult.setVerdict(com.arcguard.firesafety.diagnosis.model.Verdict.ARC);
        arcResult.setConfidence(0.97f);
        when(aiDiagnosisResultMapper.findDiagnosisResults(20L, 20, 0)).thenReturn(List.of(arcResult));
        when(aiDiagnosisResultMapper.countDiagnosisResults(20L)).thenReturn(1L);

        // when
        DiagnosisResultPageRes result = diagnosisQueryService.getDiagnosisResults(20L, new DiagnosisResultListReq());

        // then
        assertThat(result.getContent().get(0).getConfidence()).isEqualTo(0.97f);
    }

    @Test
    @DisplayName("API-017: 담당 현장이 아니면 AI 진단결과를 조회할 수 없다")
    void unassignedSiteDiagnosisResultsAreForbidden() {
        // given
        loginAs(2L, UserRole.GENERAL);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.getDiagnosisResults(20L, new DiagnosisResultListReq()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("API-017: 잘못된 페이징 조건이면 400을 반환한다")
    void invalidPageConditionFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        DiagnosisResultListReq req = new DiagnosisResultListReq();
        req.setSize(101);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.getDiagnosisResults(20L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_SIZE));
    }

    @Test
    @DisplayName("AI-001: SUPER_ADMIN은 회로의 AI 진단을 수동으로 요청할 수 있다")
    void superAdminCanTriggerManualDiagnosis() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        Circuit circuit = circuit();
        Panel panel = panel();
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());

        // when
        diagnosisQueryService.triggerManualDiagnosis(20L);

        // then
        verify(aiPredictionService).predictCircuit(panel, circuit);
    }

    @Test
    @DisplayName("AI-001: 담당 현장이 아니면 AI 진단을 수동으로 요청할 수 없다")
    void unassignedSiteManualDiagnosisIsForbidden() {
        // given
        loginAs(2L, UserRole.GENERAL);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.triggerManualDiagnosis(20L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verifyNoInteractions(aiPredictionService);
    }

    @Test
    @DisplayName("AI-001: 회로가 없으면 AI 진단을 수동으로 요청할 수 없다")
    void manualDiagnosisFailsWhenCircuitNotFound() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.triggerManualDiagnosis(20L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.CIRCUIT_NOT_FOUND));
        verifyNoInteractions(aiPredictionService);
    }

    @Test
    @DisplayName("분전반 AI 진단 현황은 권한 확인 후 최근 판정과 샘플 부족 회로를 반환한다")
    void getPanelDiagnosisSummary() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());

        PanelDiagnosisRecentRes normalResult = panelDiagnosisRecent(Verdict.NORMAL, 0.1f);
        PanelDiagnosisRecentRes arcResult = panelDiagnosisRecent(Verdict.ARC, 0.91f);
        when(aiDiagnosisResultMapper.findRecentPanelDiagnosisResults(eq(10L), any(LocalDateTime.class), eq(null), eq(200)))
                .thenReturn(List.of(normalResult));
        when(aiDiagnosisResultMapper.findRecentPanelDiagnosisResults(eq(10L), any(LocalDateTime.class), eq(Verdict.ARC.name()), eq(200)))
                .thenReturn(List.of(arcResult));
        when(aiDiagnosisResultMapper.findLatestDiagnosedAtByPanelId(10L)).thenReturn(LocalDateTime.of(2026, 8, 7, 10, 0));
        when(aiDiagnosisResultMapper.countActiveCircuitsByPanelId(10L)).thenReturn(10L);
        when(aiDiagnosisResultMapper.countDiagnosedCircuitsByPanelId(10L)).thenReturn(8L);
        when(aiDiagnosisResultMapper.countPanelDiagnosesSince(eq(10L), any(LocalDateTime.class), eq(null))).thenReturn(12L);
        when(aiDiagnosisResultMapper.countPanelDiagnosesSince(eq(10L), any(LocalDateTime.class), eq(Verdict.NORMAL.name()))).thenReturn(10L);
        when(aiDiagnosisResultMapper.countPanelDiagnosesSince(eq(10L), any(LocalDateTime.class), eq(Verdict.ARC.name()))).thenReturn(2L);
        when(aiDiagnosisResultMapper.findSampleInsufficientCircuits(10L, 30)).thenReturn(List.of());

        // when
        PanelDiagnosisSummaryRes result = diagnosisQueryService.getPanelDiagnosisSummary(10L);

        // then
        assertThat(result.getTotalCircuitCount()).isEqualTo(10L);
        assertThat(result.getDiagnosedCircuitCount()).isEqualTo(8L);
        assertThat(result.getLast24hTotalCount()).isEqualTo(12L);
        assertThat(result.getLast24hNormalCount()).isEqualTo(10L);
        assertThat(result.getLast24hArcCount()).isEqualTo(2L);
        assertThat(result.getRecentResults().get(0).getConfidence()).isEqualTo(0.9f);
        assertThat(result.getRecentArcResults().get(0).getTriggerType()).isEqualTo(DiagnosisTriggerType.AUTO);
    }

    @Test
    @DisplayName("Phase 11: SUPER_ADMIN은 AI 진단 설명을 생성/조회할 수 있고 권한 확인 후 서비스로 위임한다")
    void superAdminCanGetOrCreateExplanation() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        Circuit circuit = circuit();
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit);
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(aiDiagnosisExplanationService.getOrCreateExplanation(circuit, 100L)).thenReturn("요약 문장");

        // when
        DiagnosisExplanationRes result = diagnosisQueryService.getOrCreateExplanation(20L, 100L);

        // then
        assertThat(result.getAnalysisSummary()).isEqualTo("요약 문장");
        verify(aiDiagnosisExplanationService).getOrCreateExplanation(circuit, 100L);
    }

    @Test
    @DisplayName("Phase 11: 담당 현장이 아니면 AI 진단 설명을 생성/조회할 수 없다(외부 호출 없음)")
    void unassignedSiteExplanationIsForbidden() {
        // given
        loginAs(2L, UserRole.GENERAL);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.getOrCreateExplanation(20L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verifyNoInteractions(aiDiagnosisExplanationService);
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Circuit circuit() {
        Circuit circuit = new Circuit();
        circuit.setCircuitId(20L);
        circuit.setPanelId(10L);
        circuit.setChannelNo(1);
        return circuit;
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

    private DiagnosisResultRes diagnosisResult() {
        DiagnosisResultRes res = new DiagnosisResultRes();
        res.setResultId(100L);
        res.setCircuitId(20L);
        return res;
    }

    private PanelDiagnosisRecentRes panelDiagnosisRecent(Verdict verdict, Float confidence) {
        PanelDiagnosisRecentRes res = new PanelDiagnosisRecentRes();
        res.setResultId(200L);
        res.setCircuitId(20L);
        res.setChannelNo(1);
        res.setVerdict(verdict);
        res.setConfidence(confidence);
        res.setTriggerType(DiagnosisTriggerType.AUTO);
        return res;
    }
}
