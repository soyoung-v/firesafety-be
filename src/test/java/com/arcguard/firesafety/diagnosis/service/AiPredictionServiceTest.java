package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.diagnosis.config.AiPredictionProperties;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionSampleReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiPredictionRes;
import com.arcguard.firesafety.diagnosis.dto.res.AiPredictionResultRes;
import com.arcguard.firesafety.diagnosis.exception.DiagnosisErrorCode;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.arcguard.firesafety.diagnosis.model.AiPredictionPanelTarget;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.model.Circuit;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.monitoring.service.PanelStatusAggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPredictionServiceTest {

    @Mock
    private AiDiagnosisResultMapper aiDiagnosisResultMapper;

    @Mock
    private AiPredictionClient aiPredictionClient;

    @Mock
    private AiDiagnosisResultSaveService aiDiagnosisResultSaveService;

    @Mock
    private PanelStatusAggregationService panelStatusAggregationService;

    private AiPredictionProperties properties;
    private AiPredictionService aiPredictionService;

    @BeforeEach
    void setUp() {
        properties = new AiPredictionProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:8000");
        properties.setMinSampleSize(30);
        properties.setSampleSize(60);

        aiPredictionService = new AiPredictionService(
                aiDiagnosisResultMapper,
                aiPredictionClient,
                aiDiagnosisResultSaveService,
                panelStatusAggregationService,
                properties
        );
    }

    @Test
    @DisplayName("FR-02-01: 샘플이 충분한 회로를 AI 서버로 보내고 결과를 저장한다")
    void predictReadyPanels() {
        // given
        when(aiDiagnosisResultMapper.findPredictionPanels(30)).thenReturn(List.of(panelTarget()));
        when(aiDiagnosisResultMapper.findPredictionCircuitTargets(10L, 30)).thenReturn(List.of(circuitTarget()));
        when(aiDiagnosisResultMapper.findRecentSamples(20L, 60)).thenReturn(List.of(sample("12.3", 4)));
        when(aiPredictionClient.predict(org.mockito.Mockito.any())).thenReturn(aiResponse());

        // when
        int savedCount = aiPredictionService.predictReadyPanels();

        // then
        assertThat(savedCount).isEqualTo(1);

        ArgumentCaptor<AiPredictionReq> requestCaptor = ArgumentCaptor.forClass(AiPredictionReq.class);
        verify(aiPredictionClient).predict(requestCaptor.capture());

        AiPredictionReq request = requestCaptor.getValue();
        assertThat(request.getMNo()).isEqualTo("00001");
        assertThat(request.getCircuits()).hasSize(1);
        assertThat(request.getCircuits().get(0).getCircuit()).isEqualTo(1);
        assertThat(request.getCircuits().get(0).getSamples()).hasSize(1);

        verify(aiDiagnosisResultSaveService).save(10L, 20L, 30L, Verdict.ARC, 0.87, 60, null, DiagnosisTriggerType.AUTO);
        verify(panelStatusAggregationService).aggregatePanelStatus(10L);
    }

    @Test
    @DisplayName("AI-001: AI_PREDICTION_ENABLED가 꺼져 있으면 회로 수동 진단은 503으로 거부한다")
    void predictCircuitFailsWhenNotReady() {
        // given
        properties.setEnabled(false);

        // when & then
        assertThatThrownBy(() -> aiPredictionService.predictCircuit(panel(), circuit()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DiagnosisErrorCode.AI_PREDICTION_UNAVAILABLE));
        verifyNoInteractions(aiPredictionClient);
    }

    @Test
    @DisplayName("AI-001: 샘플이 최소 기준보다 적으면 AI 서버를 호출하지 않고 조용히 넘어간다")
    void predictCircuitSkipsWhenSamplesInsufficient() {
        // given
        when(aiDiagnosisResultMapper.findRecentSamples(20L, 60)).thenReturn(sampleListOfSize(10));

        // when
        aiPredictionService.predictCircuit(panel(), circuit());

        // then
        verifyNoInteractions(aiPredictionClient);
        verify(panelStatusAggregationService, never()).aggregatePanelStatus(any());
    }

    @Test
    @DisplayName("AI-001: 샘플이 충분하면 AI 서버를 호출해 결과를 저장하고 분전반 상태를 재집계한다")
    void predictCircuitCallsAiServerAndSavesResult() {
        // given
        when(aiDiagnosisResultMapper.findRecentSamples(20L, 60)).thenReturn(sampleListOfSize(30));
        when(aiDiagnosisResultMapper.findLatestFrameId(20L)).thenReturn(500L);
        when(aiPredictionClient.predict(any())).thenReturn(aiResponse());

        // when
        aiPredictionService.predictCircuit(panel(), circuit());

        // then
        verify(aiDiagnosisResultSaveService).save(10L, 20L, 500L, Verdict.ARC, 0.87, 60, null, DiagnosisTriggerType.MANUAL);
        verify(panelStatusAggregationService).aggregatePanelStatus(10L);
    }

    private Panel panel() {
        Panel panel = new Panel();
        panel.setPanelId(10L);
        panel.setMNo("00001");
        return panel;
    }

    private Circuit circuit() {
        Circuit circuit = new Circuit();
        circuit.setCircuitId(20L);
        circuit.setPanelId(10L);
        circuit.setChannelNo(1);
        return circuit;
    }

    private List<AiPredictionSampleReq> sampleListOfSize(int count) {
        return Collections.nCopies(count, sample("12.3", 4));
    }

    private AiPredictionPanelTarget panelTarget() {
        AiPredictionPanelTarget target = new AiPredictionPanelTarget();
        target.setPanelId(10L);
        target.setMNo("00001");
        return target;
    }

    private AiPredictionCircuitTarget circuitTarget() {
        AiPredictionCircuitTarget target = new AiPredictionCircuitTarget();
        target.setCircuitId(20L);
        target.setChannelNo(1);
        target.setLatestFrameId(30L);
        return target;
    }

    private AiPredictionSampleReq sample(String am, int count) {
        AiPredictionSampleReq sample = new AiPredictionSampleReq();
        sample.setAm(new BigDecimal(am));
        sample.setCount(count);
        return sample;
    }

    private AiPredictionRes aiResponse() {
        AiPredictionResultRes result = new AiPredictionResultRes();
        result.setCircuit(1);
        result.setPred(1);
        result.setProba(0.87);
        result.setNSamples(60);

        AiPredictionRes response = new AiPredictionRes();
        response.setMNo("00001");
        response.setResults(List.of(result));
        return response;
    }
}
