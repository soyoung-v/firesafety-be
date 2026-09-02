package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.diagnosis.config.AiExplanationProperties;
import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationSensorEvidenceReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiExplanationRes;
import com.arcguard.firesafety.diagnosis.exception.DiagnosisErrorCode;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiDiagnosisResult;
import com.arcguard.firesafety.diagnosis.model.RiskLevel;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.model.Circuit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiDiagnosisExplanationServiceTest {

    @Mock
    private AiDiagnosisResultMapper aiDiagnosisResultMapper;

    @Mock
    private AiExplanationClient aiExplanationClient;

    private AiExplanationProperties aiExplanationProperties;
    private AiDiagnosisExplanationService service;

    @BeforeEach
    void setUp() {
        aiExplanationProperties = new AiExplanationProperties();
        aiExplanationProperties.setEnabled(true);
        aiExplanationProperties.setPath("/explain");
        service = new AiDiagnosisExplanationService(aiDiagnosisResultMapper, aiExplanationClient, aiExplanationProperties);
    }

    @Test
    @DisplayName("A: AI_EXPLANATION_ENABLED=false면 FastAPI를 호출하지 않고 503을 반환한다")
    void disabledFeatureFlagSkipsFastApiCall() {
        aiExplanationProperties.setEnabled(false);
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult(null));

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_EXPLANATION_DISABLED));
        verifyNoInteractions(aiExplanationClient);
    }

    @Test
    @DisplayName("B: analysisSummary가 이미 존재하면 FastAPI를 호출하지 않고 DB 값을 그대로 반환한다")
    void existingSummaryIsReturnedWithoutFastApiCall() {
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult("기존 저장된 요약"));

        String result = service.getOrCreateExplanation(circuit(), 100L);

        assertThat(result).isEqualTo("기존 저장된 요약");
        verifyNoInteractions(aiExplanationClient);
        verify(aiDiagnosisResultMapper, never()).updateAnalysisSummaryIfAbsent(anyLong(), any());
    }

    @Test
    @DisplayName("C: summary가 없고 enabled=true면 FastAPI /explain을 정확히 1회 호출해 저장하고 반환한다")
    void generatesAndPersistsSummaryOnCacheMiss() {
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult(null));
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L))
                .thenReturn(new AiExplanationSensorEvidenceReq(5.4, 0, 78.5, 2.1, 10.4));
        when(aiExplanationClient.explain(any())).thenReturn(response("최근 전류 증가가 관찰됩니다."));
        when(aiDiagnosisResultMapper.updateAnalysisSummaryIfAbsent(100L, "최근 전류 증가가 관찰됩니다.")).thenReturn(1);

        String result = service.getOrCreateExplanation(circuit(), 100L);

        assertThat(result).isEqualTo("최근 전류 증가가 관찰됩니다.");
        verify(aiExplanationClient, times(1)).explain(any());
        verify(aiDiagnosisResultMapper).updateAnalysisSummaryIfAbsent(100L, "최근 전류 증가가 관찰됩니다.");
    }

    @Test
    @DisplayName("C-1: FastAPI로 보내는 요청은 실제 진단 결과 값을 그대로 담는다")
    void requestReflectsStoredDiagnosisResult() {
        AiDiagnosisResult diagnosisResult = diagnosisResult(null);
        diagnosisResult.setVerdict(Verdict.NORMAL);
        diagnosisResult.setConfidence(0.08f);
        diagnosisResult.setRiskLevel(RiskLevel.WARNING);
        diagnosisResult.setRiskScore(0.63f);
        diagnosisResult.setAnomaly(true);
        diagnosisResult.setAnomalyScore(0.78f);
        diagnosisResult.setPredictedCurrent(5.3f);
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult);
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L))
                .thenReturn(new AiExplanationSensorEvidenceReq(5.4, 0, 78.5, 2.1, 10.4));
        when(aiExplanationClient.explain(any())).thenReturn(response("요약"));
        when(aiDiagnosisResultMapper.updateAnalysisSummaryIfAbsent(anyLong(), any())).thenReturn(1);

        service.getOrCreateExplanation(circuit(), 100L);

        ArgumentCaptor<AiExplanationReq> captor = ArgumentCaptor.forClass(AiExplanationReq.class);
        verify(aiExplanationClient).explain(captor.capture());
        AiExplanationReq sent = captor.getValue();
        assertThat(sent.getCircuit()).isEqualTo(1);
        assertThat(sent.getPrediction().getPred()).isEqualTo(0);
        assertThat(sent.getPrediction().getProba()).isEqualTo(0.08d, org.assertj.core.data.Offset.offset(0.0001d));
        assertThat(sent.getPrediction().getRiskLevel()).isEqualTo("WARNING");
        assertThat(sent.getPrediction().getRiskScore()).isEqualTo(0.63d, org.assertj.core.data.Offset.offset(0.0001d));
        assertThat(sent.getPrediction().getAnomaly()).isTrue();
        assertThat(sent.getSensorEvidence().getTemperature()).isEqualTo(78.5d, org.assertj.core.data.Offset.offset(0.0001d));
        assertThat(sent.getSensorEvidence().getArcCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("D: FastAPI timeout이면 diagnosis는 그대로 유지되고 summary는 저장되지 않는다")
    void fastApiTimeoutLeavesResultUnchanged() {
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult(null));
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L)).thenReturn(null);
        when(aiExplanationClient.explain(any())).thenThrow(new RuntimeException("read timed out"));

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_EXPLANATION_FAILED));
        verify(aiDiagnosisResultMapper, never()).updateAnalysisSummaryIfAbsent(anyLong(), any());
    }

    @Test
    @DisplayName("E: FastAPI 502/503/504 계열 실패도 diagnosis는 유지되고 명확한 API 오류를 반환한다")
    void fastApiServerErrorReturnsExplanationFailed() {
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult(null));
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L)).thenReturn(null);
        when(aiExplanationClient.explain(any())).thenThrow(new RuntimeException("503 Service Unavailable"));

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_EXPLANATION_FAILED));
        verify(aiDiagnosisResultMapper, never()).updateAnalysisSummaryIfAbsent(anyLong(), any());
    }

    @Test
    @DisplayName("F: 빈 analysisSummary 응답은 저장하지 않는다")
    void blankSummaryIsNotPersisted() {
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult(null));
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L)).thenReturn(null);
        when(aiExplanationClient.explain(any())).thenReturn(response("   "));

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_EXPLANATION_FAILED));
        verify(aiDiagnosisResultMapper, never()).updateAnalysisSummaryIfAbsent(anyLong(), any());
    }

    @Test
    @DisplayName("G: null analysisSummary 응답은 저장하지 않는다")
    void nullSummaryIsNotPersisted() {
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult(null));
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L)).thenReturn(null);
        when(aiExplanationClient.explain(any())).thenReturn(response(null));

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_EXPLANATION_FAILED));
        verify(aiDiagnosisResultMapper, never()).updateAnalysisSummaryIfAbsent(anyLong(), any());
    }

    @Test
    @DisplayName("confidence(proba)가 없는 진단 결과는 가짜 값을 만들지 않고 명확한 오류로 거절한다")
    void missingConfidenceIsRejectedWithoutFakingValue() {
        AiDiagnosisResult diagnosisResult = diagnosisResult(null);
        diagnosisResult.setConfidence(null);
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(diagnosisResult);

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_EXPLANATION_EVIDENCE_UNAVAILABLE));
        verifyNoInteractions(aiExplanationClient);
    }

    @Test
    @DisplayName("존재하지 않는 진단 결과 요청 시 404 계열 오류를 반환한다")
    void missingDiagnosisResultThrowsNotFound() {
        when(aiDiagnosisResultMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 999L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.DIAGNOSIS_RESULT_NOT_FOUND));
        verifyNoInteractions(aiExplanationClient);
    }

    @Test
    @DisplayName("다른 회로 소속 진단 결과는 404로 거절한다")
    void diagnosisResultBelongingToOtherCircuitIsRejected() {
        AiDiagnosisResult otherCircuitResult = diagnosisResult(null);
        otherCircuitResult.setCircuitId(999L);
        when(aiDiagnosisResultMapper.findById(100L)).thenReturn(otherCircuitResult);

        assertThatThrownBy(() -> service.getOrCreateExplanation(circuit(), 100L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.DIAGNOSIS_RESULT_NOT_FOUND));
        verifyNoInteractions(aiExplanationClient);
    }

    @Test
    @DisplayName("동시 최초 요청 중 한쪽만 저장에 성공하면 이미 저장된 DB 값을 반환한다")
    void concurrentFirstRequestReturnsWhicheverWasPersistedFirst() {
        AiDiagnosisResult alreadyPersisted = diagnosisResult("먼저 저장된 응답");
        when(aiDiagnosisResultMapper.findById(100L))
                .thenReturn(diagnosisResult(null))
                .thenReturn(alreadyPersisted);
        when(aiDiagnosisResultMapper.findSensorEvidence(20L, 10L, 500L)).thenReturn(null);
        when(aiExplanationClient.explain(any())).thenReturn(response("내 응답"));
        when(aiDiagnosisResultMapper.updateAnalysisSummaryIfAbsent(100L, "내 응답")).thenReturn(0);

        String result = service.getOrCreateExplanation(circuit(), 100L);

        assertThat(result).isEqualTo("먼저 저장된 응답");
    }

    private Circuit circuit() {
        Circuit circuit = new Circuit();
        circuit.setCircuitId(20L);
        circuit.setPanelId(10L);
        circuit.setChannelNo(1);
        return circuit;
    }

    private AiDiagnosisResult diagnosisResult(String analysisSummary) {
        AiDiagnosisResult result = new AiDiagnosisResult();
        result.setResultId(100L);
        result.setCircuitId(20L);
        result.setFrameId(500L);
        result.setVerdict(Verdict.NORMAL);
        result.setConfidence(0.05f);
        result.setAnalysisSummary(analysisSummary);
        return result;
    }

    private AiExplanationRes response(String analysisSummary) {
        AiExplanationRes res = new AiExplanationRes();
        res.setAnalysisSummary(analysisSummary);
        return res;
    }
}
