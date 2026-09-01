package com.arcguard.firesafety.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationMonitorScheduler {

    private final CommunicationMonitorService communicationMonitorService;

    // 통신두절 감지 스케줄러
    // 1분 이상 미수신 분전반을 OFFLINE으로 전환하고 SYSTEM 경보를 생성한다.
    @Scheduled(
            fixedDelayString = "${monitoring.communication-lost-check-delay-ms:30000}",
            initialDelayString = "${monitoring.communication-lost-check-initial-delay-ms:30000}"
    )
    public void detectCommunicationLost() {
        int changedCount = communicationMonitorService.detectCommunicationLost();
        if (changedCount > 0) {
            log.info("통신두절 분전반 전환 완료 - count={}", changedCount);
        }
    }
}
