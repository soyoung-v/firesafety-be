package com.arcguard.firesafety.auth.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.bootstrap.admin")
public class BootstrapSuperAdminProperties {

    private boolean enabled;
    private String email;
    private String password;
    private String name;
    private String phone;
}
