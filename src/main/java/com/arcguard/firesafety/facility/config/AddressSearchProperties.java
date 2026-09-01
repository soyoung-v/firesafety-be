package com.arcguard.firesafety.facility.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.address-search")
public class AddressSearchProperties {

    private String apiKey;
    private String searchUrl;

    // API 키가 없으면 외부 호출을 시도하지 않는다.
    public boolean isReady() {
        return apiKey != null && !apiKey.isBlank();
    }
}
