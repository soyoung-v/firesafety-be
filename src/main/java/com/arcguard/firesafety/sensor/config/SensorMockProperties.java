package com.arcguard.firesafety.sensor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.sensor-mock")
public class SensorMockProperties {

    private boolean enabled;
    private long delayMs;
    private long aiDelayMs;
}
