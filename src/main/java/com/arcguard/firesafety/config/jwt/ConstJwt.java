package com.arcguard.firesafety.config.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml 또는 .env의 constants.jwt 값을 JWT/쿠키 설정으로 바인딩
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "constants.jwt")
public class ConstJwt {

    private final String issuer;
    private final String bearerFormat;
    private final String claimKey;
    private final String secretKey;
    private final String accessTokenCookieName;
    private final String accessTokenCookiePath;
    private final int accessTokenCookieValiditySeconds;
    private final long accessTokenValidityMilliseconds;
    private final String refreshTokenCookieName;
    private final String refreshTokenCookiePath;
    private final int refreshTokenCookieValiditySeconds;
    private final long refreshTokenValidityMilliseconds;
    private final boolean cookieSecure;
    private final String cookieSameSite;
}
