package com.arcguard.firesafety.config.firebase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.firebase")
public class FirebaseProperties {

    private boolean enabled;
    private String credentialsPath;

    // local/test에서는 credentialsPath가 없어도 서버가 떠야 한다.
    public boolean isReady() {
        return enabled && credentialsPath != null && !credentialsPath.isBlank();
    }
}
