package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.mapper.AlertMapper;
import com.arcguard.firesafety.alert.model.Alert;
import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemAlertService {

    private final AlertMapper alertMapper;
    private final AlertNotificationPublisher alertNotificationPublisher;

    // 통신두절 SYSTEM 경보 생성
    // 이미 미조치 통신두절 경보가 있으면 같은 경보를 반복해서 쌓지 않는다.
    @Transactional
    public void createCommunicationLostAlert(Long panelId) {
        boolean exists = alertMapper.existsUnresolvedAlert(
                panelId,
                AlertSource.SYSTEM.name(),
                AlertType.COMM_LOST.name(),
                AlertSeverity.RISK.name()
        );
        if (exists) {
            return;
        }

        Alert alert = new Alert();
        alert.setPanelId(panelId);
        alert.setSource(AlertSource.SYSTEM);
        alert.setType(AlertType.COMM_LOST);
        alert.setSeverity(AlertSeverity.RISK);
        alert.setStatus(AlertStatus.UNCONFIRMED);
        alertMapper.insertAlert(alert);
        alertNotificationPublisher.publishCreated(alert);
    }
}
