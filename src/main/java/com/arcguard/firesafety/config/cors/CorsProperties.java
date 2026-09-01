package com.arcguard.firesafety.config.cors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// application.yml 또는 .env의 constants.cors 값을 CORS/WebSocket 허용 Origin으로 바인딩
@Getter
@Setter
@ConfigurationProperties(prefix = "constants.cors")
public class CorsProperties {

    private List<String> allowedOrigins;
}
