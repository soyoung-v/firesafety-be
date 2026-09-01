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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorIngestService {

    private static final int MAX_CHANNEL_COUNT = 10;
    private static final Pattern HEX_8 = Pattern.compile("^[0-9A-Fa-f]{8}$");

    private final PanelMapper panelMapper;
    private final CircuitMapper circuitMapper;
    private final SensorFrameMapper sensorFrameMapper;
    private final SensorFrameCircuitMapper sensorFrameCircuitMapper;
    private final DeviceAlertService deviceAlertService;
    private final PanelStatusAggregationService panelStatusAggregationService;
    private final MonitoringRefreshPublisher monitoringRefreshPublisher;

    // 디바이스 프레임 수신
    // 1. 자리수 검증 → 2. 분전반 조회 → 3. 프레임 저장 → 4. 회로별 값 저장 → 5. 통신시각 갱신
    @Transactional
    public SensorFrameIngestRes ingest(Map<String, String> params) {
        try {
            validateRequiredParams(params);

            Panel panel = findPanel(params.get("m_no"));
            int circuitCount = resolveCircuitCount(panel);
            SensorFrame sensorFrame = buildSensorFrame(panel.getPanelId(), params);

            sensorFrameMapper.insertSensorFrame(sensorFrame);
            Map<Integer, Long> circuitIdsByChannelNo = saveCircuitFrames(sensorFrame.getFrameId(), panel.getPanelId(), circuitCount, params);
            deviceAlertService.createDeviceAlerts(panel.getPanelId(), params.get("aerror"), circuitIdsByChannelNo);
            panelStatusAggregationService.aggregatePanelStatus(panel.getPanelId());
            panelMapper.updatePanelCommunication(panel.getPanelId());
            monitoringRefreshPublisher.publish(panel.getSiteId(), "SENSOR_FRAME_RECEIVED");

            // Mapper insert 후 received_at은 DB 기본값이므로 응답용 현재 시각을 채워준다.
            sensorFrame.setReceivedAt(LocalDateTime.now());
            return SensorFrameIngestRes.from(sensorFrame);
        } catch (BusinessException e) {
            // 파싱/매핑 실패 시 원본 쿼리값을 애플리케이션 로그에 남겨 장비 전송값을 추적한다.
            log.warn("센서 프레임 수신 실패 - params={}", params);
            throw e;
        }
    }

    // 프로토콜 고정 필드의 누락/자리수 확인
    private void validateRequiredParams(Map<String, String> params) {
        validateLength(params, "m_no", 5);
        validateLength(params, "mode", 1);
        validateLength(params, "volt", 3);
        validateLength(params, "hct_count", 4);
        validateLength(params, "s_circuit", 2);
        validateSignedLength(params, "tem", 3);
        validateSignedLength(params, "humi", 3);
        validateLength(params, "fire", 4);
        validateLength(params, "gas", 4);
        validateLength(params, "door", 1);
        validateLength(params, "total_circuit", 2);
        validateLength(params, "e_energy", 5);
        validateAerror(params.get("aerror"));

        for (int channelNo = 1; channelNo <= MAX_CHANNEL_COUNT; channelNo++) {
            validateLength(params, "am" + channelNo, 3);
            validateLength(params, "count" + channelNo, 4);
        }
    }

    // 누락되거나 자리수가 맞지 않으면 프레임 전체를 저장하지 않는다.
    private void validateLength(Map<String, String> params, String name, int length) {
        String value = params.get(name);
        if (value == null || value.length() != length) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
    }

    // tem/humi는 영하 값(부호 포함)이 올 수 있어 부호는 제외하고 나머지 자리수만 검증한다
    private void validateSignedLength(Map<String, String> params, String name, int length) {
        String value = params.get(name);
        if (value == null) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
        String digits = value.startsWith("-") ? value.substring(1) : value;
        if (digits.length() != length) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
    }

    // aerror는 byte0~byte3을 16진수 8자리로 전달받는다.
    private void validateAerror(String aerror) {
        if (aerror == null || !HEX_8.matcher(aerror).matches()) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
    }

    // m_no 기준 활성 분전반 조회
    private Panel findPanel(String mNo) {
        Panel panel = panelMapper.findActivePanelByMNo(mNo);
        if (panel == null) {
            throw new BusinessException(SensorErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
    }

    // 저장 대상 회로 수는 분전반 설정값을 기준으로 하되 물리 최대 10을 넘지 않는다.
    private int resolveCircuitCount(Panel panel) {
        if (panel.getCircuitCount() == null || panel.getCircuitCount() < 1 || panel.getCircuitCount() > MAX_CHANNEL_COUNT) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
        return panel.getCircuitCount();
    }

    // sensor_frame 공통 필드 생성
    private SensorFrame buildSensorFrame(Long panelId, Map<String, String> params) {
        SensorFrame sensorFrame = new SensorFrame();
        sensorFrame.setPanelId(panelId);
        sensorFrame.setMode(parseInt(params.get("mode")));
        sensorFrame.setVoltV(parseDecimal(params.get("volt")));
        sensorFrame.setLeakMa(parseDecimal(params.get("s_circuit")));
        sensorFrame.setTemperature(parseDecimalByTen(params.get("tem")));
        sensorFrame.setHumidity(parseDecimalByTen(params.get("humi")));
        sensorFrame.setFireRaw(parseInt(params.get("fire")));
        sensorFrame.setGasRaw(parseInt(params.get("gas")));
        sensorFrame.setErrorBits(params.get("aerror"));
        sensorFrame.setDoorStatus(parseDoorStatus(params.get("door")));
        sensorFrame.setTotalCurrent(parseDecimal(params.get("total_circuit")));
        sensorFrame.setTotalPower(parseInt(params.get("e_energy")));
        return sensorFrame;
    }

    // 회로별 am/count와 하드웨어 ARC bit 저장
    // 회로가 삭제/미등록인 채널은 건너뛴다 — 채널 하나가 없다고 나머지 채널·프레임 전체가 실패하면 안 됨
    // (회로 삭제 후 panel.circuit_count가 안 줄어든 경우에도 분전반 통신 자체는 계속 정상 처리되게 함)
    private Map<Integer, Long> saveCircuitFrames(Long frameId, Long panelId, int circuitCount, Map<String, String> params) {
        Map<Integer, Circuit> circuitsByChannelNo = circuitMapper.findActiveCircuitsByPanelId(panelId).stream()
                .collect(Collectors.toMap(Circuit::getChannelNo, Function.identity()));

        Map<Integer, Long> circuitIdsByChannelNo = new LinkedHashMap<>();
        for (int channelNo = 1; channelNo <= circuitCount; channelNo++) {
            Circuit circuit = circuitsByChannelNo.get(channelNo);
            if (circuit == null) {
                continue;
            }
            circuitIdsByChannelNo.put(channelNo, circuit.getCircuitId());

            SensorFrameCircuit frameCircuit = new SensorFrameCircuit();
            frameCircuit.setFrameId(frameId);
            frameCircuit.setCircuitId(circuit.getCircuitId());
            frameCircuit.setCurrentA(parseDecimalByTen(params.get("am" + channelNo)));
            frameCircuit.setArcCounter(parseInt(params.get("count" + channelNo)));
            frameCircuit.setDeviceArcFlag(isDeviceArc(params.get("aerror"), channelNo));

            sensorFrameCircuitMapper.insertSensorFrameCircuit(frameCircuit);
        }
        return circuitIdsByChannelNo;
    }

    // door 0은 닫힘(false), 1은 열림(true)
    private Boolean parseDoorStatus(String value) {
        if ("0".equals(value)) {
            return false;
        }
        if ("1".equals(value)) {
            return true;
        }
        throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
    }

    // 정수 문자열 파싱
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
    }

    // 단위 변환이 없는 수치 파싱
    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(SensorErrorCode.INVALID_FRAME_PARAMETER);
        }
    }

    // 0.1 단위로 들어오는 값은 실제 단위로 변환해서 저장
    private BigDecimal parseDecimalByTen(String value) {
        return parseDecimal(value).movePointLeft(1);
    }

    // aerror byte0/byte1의 ARC bit를 회로 번호로 매핑
    private boolean isDeviceArc(String aerror, int channelNo) {
        int byteIndex = channelNo <= 5 ? 0 : 1;
        int bitIndex = channelNo <= 5 ? channelNo - 1 : channelNo - 6;
        int byteValue = Integer.parseInt(aerror.substring(byteIndex * 2, byteIndex * 2 + 2), 16);
        return (byteValue & (1 << bitIndex)) != 0;
    }
}
