package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.alert.service.AiAlertService;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiDiagnosisResult;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.RiskLevel;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiDiagnosisResultSaveServiceTest {

    @Mock
    private AiDiagnosisResultMapper aiDiagnosisResultMapper;

    @Mock
    private AiAlertService aiAlertService;

    private AiDiagnosisResultSaveService aiDiagnosisResultSaveService;

    @BeforeEach
    void setUp() {
        aiDiagnosisResultSaveService = new AiDiagnosisResultSaveService(aiDiagnosisResultMapper, aiAlertService);
    }

    @Test
    @DisplayName("FR-02-01: AI ARC 결과 저장 후 경보를 생성한다")
    void saveArcResultAndCreateAlert() {
        // given
        doAnswer(invocation -> {
            AiDiagnosisResult result = invocation.getArgument(0);
            result.setResultId(100L);
            return null;
        }).when(aiDiagnosisResultMapper).insertAiDiagnosisResult(org.mockito.Mockito.any());

        // when
        aiDiagnosisResultSaveService.save(
                10L, 20L, 30L, Verdict.ARC, 0.91, 60, null, DiagnosisTriggerType.MANUAL,
                null, null, null, null, null
        );

        // then
        ArgumentCaptor<AiDiagnosisResult> resultCaptor = ArgumentCaptor.forClass(AiDiagnosisResult.class);
        verify(aiDiagnosisResultMapper).insertAiDiagnosisResult(resultCaptor.capture());
        verify(aiAlertService).createArcAlert(10L, 20L, 100L);

        AiDiagnosisResult result = resultCaptor.getValue();
        assertThat(result.getCircuitId()).isEqualTo(20L);
        assertThat(result.getFrameId()).isEqualTo(30L);
        assertThat(result.getVerdict()).isEqualTo(Verdict.ARC);
        assertThat(result.getConfidence()).isEqualTo(0.91f);
        assertThat(result.getNSamples()).isEqualTo(60);
        assertThat(result.getWarning()).isNull();
        assertThat(result.getTriggerType()).isEqualTo(DiagnosisTriggerType.MANUAL);
    }

    @Test
    @DisplayName("FR-02-01: AI NORMAL 결과는 저장만 하고 경보를 만들지 않는다")
    void saveNormalResultWithoutAlert() {
        // when
        aiDiagnosisResultSaveService.save(
                10L, 20L, 30L, Verdict.NORMAL, null, 45,
                "샘플 45개 — 60개 이상 권장 (정확도 저하 가능)", DiagnosisTriggerType.AUTO,
                null, null, null, null, null
        );

        // then
        ArgumentCaptor<AiDiagnosisResult> resultCaptor = ArgumentCaptor.forClass(AiDiagnosisResult.class);
        verify(aiDiagnosisResultMapper).insertAiDiagnosisResult(resultCaptor.capture());
        verify(aiAlertService, never()).createArcAlert(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyLong());

        AiDiagnosisResult result = resultCaptor.getValue();
        assertThat(result.getVerdict()).isEqualTo(Verdict.NORMAL);
        assertThat(result.getConfidence()).isNull();
        assertThat(result.getNSamples()).isEqualTo(45);
        assertThat(result.getWarning()).isEqualTo("샘플 45개 — 60개 이상 권장 (정확도 저하 가능)");
        assertThat(result.getTriggerType()).isEqualTo(DiagnosisTriggerType.AUTO);
    }

    @Test
    @DisplayName("Phase 9: riskLevel=DANGER, anomaly=true여도 ARC 경보를 생성하지 않는다 (ADR-002 모델 역할 분리)")
    void saveDangerAndAnomalyResultWithoutForcedArcAlert() {
        // when
        aiDiagnosisResultSaveService.save(
                10L, 20L, 30L, Verdict.NORMAL, 0.05, 60, null, DiagnosisTriggerType.AUTO,
                RiskLevel.DANGER, 0.93, true, 0.87, 5.12
        );

        // then
        verify(aiAlertService, never()).createArcAlert(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyLong());

        ArgumentCaptor<AiDiagnosisResult> resultCaptor = ArgumentCaptor.forClass(AiDiagnosisResult.class);
        verify(aiDiagnosisResultMapper).insertAiDiagnosisResult(resultCaptor.capture());

        AiDiagnosisResult result = resultCaptor.getValue();
        assertThat(result.getVerdict()).isEqualTo(Verdict.NORMAL);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(result.getRiskScore()).isEqualTo(0.93f);
        assertThat(result.getAnomaly()).isTrue();
        assertThat(result.getAnomalyScore()).isEqualTo(0.87f);
        assertThat(result.getPredictedCurrent()).isEqualTo(5.12f);
    }

    @Test
    @DisplayName("Phase 9: context가 없어 신규 확장 필드가 전부 null이어도 기존 ARC 저장은 그대로 성공한다")
    void saveArcResultWithNullExtendedFields() {
        // given
        doAnswer(invocation -> {
            AiDiagnosisResult result = invocation.getArgument(0);
            result.setResultId(200L);
            return null;
        }).when(aiDiagnosisResultMapper).insertAiDiagnosisResult(org.mockito.Mockito.any());

        // when
        aiDiagnosisResultSaveService.save(
                10L, 20L, 30L, Verdict.ARC, 0.87, 60, null, DiagnosisTriggerType.AUTO,
                null, null, null, null, null
        );

        // then
        verify(aiAlertService).createArcAlert(10L, 20L, 200L);

        ArgumentCaptor<AiDiagnosisResult> resultCaptor = ArgumentCaptor.forClass(AiDiagnosisResult.class);
        verify(aiDiagnosisResultMapper).insertAiDiagnosisResult(resultCaptor.capture());

        AiDiagnosisResult result = resultCaptor.getValue();
        assertThat(result.getVerdict()).isEqualTo(Verdict.ARC);
        assertThat(result.getRiskLevel()).isNull();
        assertThat(result.getRiskScore()).isNull();
        assertThat(result.getAnomaly()).isNull();
        assertThat(result.getAnomalyScore()).isNull();
        assertThat(result.getPredictedCurrent()).isNull();
    }
}
