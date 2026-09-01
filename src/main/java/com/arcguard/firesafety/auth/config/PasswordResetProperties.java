package com.arcguard.firesafety.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.password-reset")
public class PasswordResetProperties {

    private String baseUrl;
    private int tokenExpirationMinutes;
    private String mailFromAddress;
    private String mailFromName;
    private int rateLimitWindowMinutes;
    private int rateLimitMaxCount;
}
