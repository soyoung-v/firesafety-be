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
public class AiAlertService {

    private final AlertMapper alertMapper;
    private final AlertNotificationPublisher alertNotificationPublisher;

    // AI ARC 경보 생성
    // 같은 회로에 미조치 AI ARC 경보가 있으면 반복 생성하지 않는다.
    @Transactional
    public void createArcAlert(Long panelId, Long circuitId, Long resultId) {
        boolean exists = alertMapper.existsUnresolvedCircuitAlert(
                circuitId,
                AlertSource.AI.name(),
                AlertType.ARC.name(),
                AlertSeverity.CAUTION.name()
        );
        if (exists) {
            return;
        }

        Alert alert = new Alert();
        alert.setPanelId(panelId);
        alert.setCircuitId(circuitId);
        alert.setResultId(resultId);
        alert.setSource(AlertSource.AI);
        alert.setType(AlertType.ARC);
        alert.setSeverity(AlertSeverity.CAUTION);
        alert.setStatus(AlertStatus.UNCONFIRMED);
        alertMapper.insertAlert(alert);
        alertNotificationPublisher.publishCreated(alert);
    }
}
