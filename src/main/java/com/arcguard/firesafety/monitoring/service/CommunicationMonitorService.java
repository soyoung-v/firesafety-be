package com.arcguard.firesafety.monitoring.service;

import com.arcguard.firesafety.alert.service.SystemAlertService;
import com.arcguard.firesafety.monitoring.mapper.CommunicationMonitorMapper;
import com.arcguard.firesafety.monitoring.model.OfflinePanelTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunicationMonitorService {

    private static final long COMMUNICATION_LOST_MINUTES = 1L;

    private final CommunicationMonitorMapper communicationMonitorMapper;
    private final SystemAlertService systemAlertService;
    private final Clock clock;

    // 통신두절 대상 감지
    @Transactional
    public int detectCommunicationLost() {
        LocalDateTime thresholdAt = LocalDateTime.now(clock).minusMinutes(COMMUNICATION_LOST_MINUTES);
        List<OfflinePanelTarget> targets = communicationMonitorMapper.findOfflineTargets(thresholdAt);

        int changedCount = 0;
        for (OfflinePanelTarget target : targets) {
            changedCount += markOfflineAndCreateAlert(target.getPanelId());
        }
        return changedCount;
    }

    // 대상별 상태 전환 + SYSTEM 경보 생성
    private int markOfflineAndCreateAlert(Long panelId) {
        int updatedRows = communicationMonitorMapper.markPanelOffline(panelId);
        if (updatedRows == 0) {
            return 0;
        }
        systemAlertService.createCommunicationLostAlert(panelId);
        return 1;
    }
}
