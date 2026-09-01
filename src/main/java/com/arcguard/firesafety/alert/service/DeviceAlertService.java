package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.mapper.AlertMapper;
import com.arcguard.firesafety.alert.model.Alert;
import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeviceAlertService {

    private static final int BYTE_0_ARC = 0;
    private static final int BYTE_1_ARC = 1;
    private static final int BYTE_2_ERROR = 2;
    private static final int BYTE_3_ALARM = 3;

    private final AlertMapper alertMapper;
    private final AlertNotificationPublisher alertNotificationPublisher;

    // 하드웨어 aerror 위험 bit를 DEVICE 경보로 저장
    public void createDeviceAlerts(Long panelId, String aerror, Map<Integer, Long> circuitIdsByChannelNo) {
        List<Alert> alerts = new ArrayList<>();
        alerts.addAll(createArcAlerts(panelId, aerror, circuitIdsByChannelNo));
        alerts.addAll(createDeviceErrorAlerts(panelId, aerror));
        alerts.addAll(createAlarmAlerts(panelId, aerror));

        for (Alert alert : alerts) {
            if (hasUnresolvedRiskAlert(alert)) {
                continue;
            }
            alertMapper.insertAlert(alert);
            alertNotificationPublisher.publishCreated(alert);
        }
    }

    private boolean hasUnresolvedRiskAlert(Alert alert) {
        if (alert.getCircuitId() != null) {
            return alertMapper.existsUnresolvedCircuitAlert(
                    alert.getCircuitId(),
                    alert.getSource().name(),
                    alert.getType().name(),
                    alert.getSeverity().name()
            );
        }
        return alertMapper.existsUnresolvedAlert(
                alert.getPanelId(),
                alert.getSource().name(),
                alert.getType().name(),
                alert.getSeverity().name()
        );
    }

    // byte0/byte1 ARC bit는 회로별 아크 경보
    private List<Alert> createArcAlerts(Long panelId, String aerror, Map<Integer, Long> circuitIdsByChannelNo) {
        List<Alert> alerts = new ArrayList<>();
        for (int channelNo = 1; channelNo <= 10; channelNo++) {
            if (isArcBitOn(aerror, channelNo) && circuitIdsByChannelNo.containsKey(channelNo)) {
                alerts.add(newAlert(panelId, circuitIdsByChannelNo.get(channelNo), AlertType.ARC));
            }
        }
        return alerts;
    }

    // byte2 ERROR bit는 장비 자체 이상으로 묶어서 기록
    private List<Alert> createDeviceErrorAlerts(Long panelId, String aerror) {
        List<Alert> alerts = new ArrayList<>();
        int byteValue = parseByte(aerror, BYTE_2_ERROR);
        if (hasBit(byteValue, 0) || hasBit(byteValue, 1) || hasBit(byteValue, 2)) {
            alerts.add(newAlert(panelId, null, AlertType.DEVICE_ERROR));
        }
        return alerts;
    }

    // byte3 ALARM bit는 항목별 경보 타입으로 기록
    private List<Alert> createAlarmAlerts(Long panelId, String aerror) {
        List<Alert> alerts = new ArrayList<>();
        int byteValue = parseByte(aerror, BYTE_3_ALARM);

        addPanelAlertIfBitOn(alerts, panelId, byteValue, 0, AlertType.LEAKAGE);
        addPanelAlertIfBitOn(alerts, panelId, byteValue, 1, AlertType.OVERHEAT);
        addPanelAlertIfBitOn(alerts, panelId, byteValue, 2, AlertType.HUMIDITY);
        addPanelAlertIfBitOn(alerts, panelId, byteValue, 3, AlertType.GAS);
        addPanelAlertIfBitOn(alerts, panelId, byteValue, 4, AlertType.FIRE);
        addPanelAlertIfBitOn(alerts, panelId, byteValue, 5, AlertType.DOOR_OPEN);
        addPanelAlertIfBitOn(alerts, panelId, byteValue, 6, AlertType.OVERCURRENT);

        return alerts;
    }

    // bit가 켜진 경우에만 분전반 단위 경보 추가
    private void addPanelAlertIfBitOn(List<Alert> alerts, Long panelId, int byteValue, int bitIndex, AlertType type) {
        if (hasBit(byteValue, bitIndex)) {
            alerts.add(newAlert(panelId, null, type));
        }
    }

    // alert row 기본값 생성
    private Alert newAlert(Long panelId, Long circuitId, AlertType type) {
        Alert alert = new Alert();
        alert.setPanelId(panelId);
        alert.setCircuitId(circuitId);
        alert.setSource(AlertSource.DEVICE);
        alert.setType(type);
        alert.setSeverity(AlertSeverity.RISK);
        alert.setStatus(AlertStatus.UNCONFIRMED);
        return alert;
    }

    // 회로 번호를 byte0/byte1의 ARC bit 위치로 변환
    private boolean isArcBitOn(String aerror, int channelNo) {
        int byteIndex = channelNo <= 5 ? BYTE_0_ARC : BYTE_1_ARC;
        int bitIndex = channelNo <= 5 ? channelNo - 1 : channelNo - 6;
        return hasBit(parseByte(aerror, byteIndex), bitIndex);
    }

    // aerror 8자리에서 1바이트를 16진수로 파싱
    private int parseByte(String aerror, int byteIndex) {
        return Integer.parseInt(aerror.substring(byteIndex * 2, byteIndex * 2 + 2), 16);
    }

    // 지정 bit가 켜져 있는지 확인
    private boolean hasBit(int byteValue, int bitIndex) {
        return (byteValue & (1 << bitIndex)) != 0;
    }
}
