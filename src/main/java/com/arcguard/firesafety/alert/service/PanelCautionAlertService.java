package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.mapper.AlertMapper;
import com.arcguard.firesafety.alert.model.Alert;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PanelCautionAlertService {

    private final AlertMapper alertMapper;
    private final AlertNotificationPublisher alertNotificationPublisher;

    // 서버가 계산한 CAUTION 전환 알림. AI ARC는 AiAlertService가 회로별로 이미 생성하므로 여기서는 센서/임계값 주의만 다룬다.
    public void createPanelCautionAlerts(Long panelId, List<AlertType> types) {
        if (panelId == null || types == null || types.isEmpty()) {
            return;
        }

        types.stream().distinct().forEach(type -> createPanelCautionAlert(panelId, type));
    }

    private void createPanelCautionAlert(Long panelId, AlertType type) {
        boolean exists = alertMapper.existsUnresolvedAlert(
                panelId,
                AlertSource.SYSTEM.name(),
                type.name(),
                AlertSeverity.CAUTION.name()
        );
        if (exists) {
            return;
        }

        Alert alert = new Alert();
        alert.setPanelId(panelId);
        alert.setSource(AlertSource.SYSTEM);
        alert.setType(type);
        alert.setSeverity(AlertSeverity.CAUTION);
        alert.setStatus(AlertStatus.UNCONFIRMED);
        alertMapper.insertAlert(alert);
        alertNotificationPublisher.publishCreated(alert);
    }
}
