package com.arcguard.firesafety.diagnosis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.ai-prediction")
public class AiPredictionProperties {

    private boolean enabled;
    private String baseUrl;
    private String path;
    private int minSampleSize;
    private int sampleSize;
    private long schedulerDelayMs;
    private long schedulerInitialDelayMs;

    // AI 서버 주소가 없으면 스케줄러가 호출을 건너뛴다.
    public boolean isReady() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }
}
