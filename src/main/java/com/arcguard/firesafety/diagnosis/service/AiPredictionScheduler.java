package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.diagnosis.config.AiPredictionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiPredictionScheduler {

    private final AiPredictionService aiPredictionService;
    private final AiPredictionProperties aiPredictionProperties;

    // AI 예측 스케줄러
    // 회로별 새 샘플이 충분히 쌓였을 때만 외부 AI 서버를 호출한다.
    @Scheduled(
            fixedDelayString = "${constants.ai-prediction.scheduler-delay-ms:60000}",
            initialDelayString = "${constants.ai-prediction.scheduler-initial-delay-ms:60000}"
    )
    public void predict() {
        if (!aiPredictionProperties.isReady()) {
            return;
        }

        try {
            int savedCount = aiPredictionService.predictReadyPanels();
            if (savedCount > 0) {
                log.info("AI 예측 결과 저장 완료 - count={}", savedCount);
            }
        } catch (RuntimeException e) {
            // 외부 AI 서버 장애가 백엔드 전체 스케줄러를 중단시키지 않도록 로그만 남긴다.
            log.warn("AI 예측 스케줄러 실패 - message={}", e.getMessage());
        }
    }
}
