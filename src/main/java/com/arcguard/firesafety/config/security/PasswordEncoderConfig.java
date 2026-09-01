package com.arcguard.firesafety.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// NFR-02 기준 비밀번호 BCrypt 해시 설정
@Configuration
public class PasswordEncoderConfig {

    // 비밀번호 해시 인코더 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
