package com.arcguard.firesafety.config.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    // 시간 기준을 테스트에서 교체하기 쉽게 Clock으로 주입
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
