package com.arcguard.firesafety.diagnosis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// LLM 진단 설명(POST /explain) 기능 flag. base-url은 constants.ai-prediction.base-url을 그대로
// 재사용한다(같은 FastAPI 서버) - 여기서는 중복 base-url을 만들지 않는다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.ai-explanation")
public class AiExplanationProperties {

    private boolean enabled;
    private String path;
}
