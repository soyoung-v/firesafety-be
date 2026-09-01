package com.arcguard.firesafety.sensor.service;

import com.arcguard.firesafety.alert.service.DeviceAlertService;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.facility.mapper.CircuitMapper;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.model.Circuit;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.monitoring.service.PanelStatusAggregationService;
import com.arcguard.firesafety.monitoring.service.MonitoringRefreshPublisher;
import com.arcguard.firesafety.sensor.dto.res.SensorFrameIngestRes;
import com.arcguard.firesafety.sensor.exception.SensorErrorCode;
import com.arcguard.firesafety.sensor.mapper.SensorFrameCircuitMapper;
import com.arcguard.firesafety.sensor.mapper.SensorFrameMapper;
import com.arcguard.firesafety.sensor.model.SensorFrame;
import com.arcguard.firesafety.sensor.model.SensorFrameCircuit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorIngestServiceTest {

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private CircuitMapper circuitMapper;

    @Mock
    private SensorFrameMapper sensorFrameMapper;

    @Mock
    private SensorFrameCircuitMapper sensorFrameCircuitMapper;

    @Mock
    private DeviceAlertService deviceAlertService;

    @Mock
    private PanelStatusAggregationService panelStatusAggregationService;

    @Mock
    private MonitoringRefreshPublisher monitoringRefreshPublisher;

    private SensorIngestService sensorIngestService;

    @BeforeEach
    void setUp() {
        sensorIngestService = new SensorIngestService(
                panelMapper,
                circuitMapper,
                sensorFrameMapper,
                sensorFrameCircuitMapper,
                deviceAlertService,
                panelStatusAggregationService,
                monitoringRefreshPublisher
        );
    }

    @Test
    @DisplayName("API-016: 디바이스 수신값을 프레임과 회로별 측정값으로 저장한다")
    void ingestSensorFrame() {
        // given
        Map<String, String> params = validParams();
        Panel panel = panel(10L, 5);
        when(panelMapper.findActivePanelByMNo("00001")).thenReturn(panel);
        when(circuitMapper.findActiveCircuitsByPanelId(10L))
                .thenReturn(java.util.List.of(circuit(1), circuit(2), circuit(3), circuit(4), circuit(5)));
        doAnswer(invocation -> {
            SensorFrame sensorFrame = invocation.getArgument(0);
            sensorFrame.setFrameId(100L);
            return null;
        }).when(sensorFrameMapper).insertSensorFrame(any(SensorFrame.class));

        // when
        SensorFrameIngestRes result = sensorIngestService.ingest(params);

        // then
        assertThat(result.getFrameId()).isEqualTo(100L);

        ArgumentCaptor<SensorFrame> frameCaptor = ArgumentCaptor.forClass(SensorFrame.class);
        verify(sensorFrameMapper).insertSensorFrame(frameCaptor.capture());
        assertThat(frameCaptor.getValue().getPanelId()).isEqualTo(10L);
        assertThat(frameCaptor.getValue().getTemperature()).isEqualByComparingTo(new BigDecimal("27.2"));
        assertThat(frameCaptor.getValue().getHumidity()).isEqualByComparingTo(new BigDecimal("48.4"));
        assertThat(frameCaptor.getValue().getDoorStatus()).isTrue();

        ArgumentCaptor<SensorFrameCircuit> circuitCaptor = ArgumentCaptor.forClass(SensorFrameCircuit.class);
        verify(sensorFrameCircuitMapper, org.mockito.Mockito.times(5)).insertSensorFrameCircuit(circuitCaptor.capture());
        assertThat(circuitCaptor.getAllValues().get(0).getCurrentA()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(circuitCaptor.getAllValues().get(3).getDeviceArcFlag()).isTrue();
        assertThat(circuitCaptor.getAllValues().get(4).getDeviceArcFlag()).isTrue();
        verify(deviceAlertService).createDeviceAlerts(
                org.mockito.Mockito.eq(10L),
                org.mockito.Mockito.eq("18000000"),
                org.mockito.Mockito.anyMap()
        );
        verify(panelStatusAggregationService).aggregatePanelStatus(10L);
        verify(panelMapper).updatePanelCommunication(10L);
        verify(monitoringRefreshPublisher).publish(3L, "SENSOR_FRAME_RECEIVED");
    }

    @Test
    @DisplayName("API-016: aerror가 8자리 16진수가 아니면 400을 반환한다")
    void invalidAerrorFails() {
        // given
        Map<String, String> params = validParams();
        params.put("aerror", "123");

        // when & then
        assertThatThrownBy(() -> sensorIngestService.ingest(params))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SensorErrorCode.INVALID_FRAME_PARAMETER));

        verify(sensorFrameMapper, never()).insertSensorFrame(any());
    }

    @Test
    @DisplayName("API-016: 영하 온습도(부호 포함 4자리)는 부호를 제외한 자리수로 검증해 정상 저장한다")
    void negativeTemperatureAndHumidityAreAccepted() {
        // given
        Map<String, String> params = validParams();
        params.put("tem", "-050");
        params.put("humi", "-010");
        Panel panel = panel(10L, 5);
        when(panelMapper.findActivePanelByMNo("00001")).thenReturn(panel);
        when(circuitMapper.findActiveCircuitsByPanelId(10L))
                .thenReturn(java.util.List.of(circuit(1), circuit(2), circuit(3), circuit(4), circuit(5)));
        doAnswer(invocation -> {
            SensorFrame sensorFrame = invocation.getArgument(0);
            sensorFrame.setFrameId(100L);
            return null;
        }).when(sensorFrameMapper).insertSensorFrame(any(SensorFrame.class));

        // when
        sensorIngestService.ingest(params);

        // then
        ArgumentCaptor<SensorFrame> frameCaptor = ArgumentCaptor.forClass(SensorFrame.class);
        verify(sensorFrameMapper).insertSensorFrame(frameCaptor.capture());
        assertThat(frameCaptor.getValue().getTemperature()).isEqualByComparingTo(new BigDecimal("-5.0"));
        assertThat(frameCaptor.getValue().getHumidity()).isEqualByComparingTo(new BigDecimal("-1.0"));
    }

    @Test
    @DisplayName("API-016: 온습도가 부호를 제외하고 3자리가 아니면 400을 반환한다")
    void temperatureWithWrongDigitCountFails() {
        // given
        Map<String, String> params = validParams();
        params.put("tem", "-99");

        // when & then
        assertThatThrownBy(() -> sensorIngestService.ingest(params))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SensorErrorCode.INVALID_FRAME_PARAMETER));

        verify(sensorFrameMapper, never()).insertSensorFrame(any());
    }

    @Test
    @DisplayName("API-016: 장비번호에 해당하는 분전반이 없으면 저장하지 않는다")
    void missingPanelFails() {
        // given
        Map<String, String> params = validParams();
        when(panelMapper.findActivePanelByMNo("00001")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> sensorIngestService.ingest(params))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SensorErrorCode.PANEL_NOT_FOUND));

        verify(sensorFrameMapper, never()).insertSensorFrame(any());
    }

    private Map<String, String> validParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("m_no", "00001");
        params.put("mode", "0");
        params.put("volt", "230");
        for (int channelNo = 1; channelNo <= 10; channelNo++) {
            params.put("am" + channelNo, "000");
            params.put("count" + channelNo, "0000");
        }
        params.put("hct_count", "0005");
        params.put("s_circuit", "20");
        params.put("tem", "272");
        params.put("humi", "484");
        params.put("fire", "3923");
        params.put("gas", "2212");
        params.put("aerror", "18000000");
        params.put("door", "1");
        params.put("total_circuit", "30");
        params.put("e_energy", "00000");
        return params;
    }

    private Panel panel(Long panelId, int circuitCount) {
        Panel panel = new Panel();
        panel.setPanelId(panelId);
        panel.setSiteId(3L);
        panel.setCircuitCount(circuitCount);
        return panel;
    }

    private Circuit circuit(int channelNo) {
        Circuit circuit = new Circuit();
        circuit.setCircuitId((long) channelNo);
        circuit.setChannelNo(channelNo);
        return circuit;
    }
}
