package com.arcguard.firesafety.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ACCESS_TOKEN_COOKIE = "atCookie";
    public static final String REFRESH_TOKEN_COOKIE = "rtCookie";

    // Swagger 전역 설명과 HttpOnly Cookie 기반 JWT 인증 방식을 문서화
    @Bean
    public OpenAPI firesafetyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ArcGuard API")
                        .description("JWT는 응답 body가 아니라 at/rt HttpOnly Cookie로 전달한다.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN_COOKIE, cookieScheme("at"))
                        .addSecuritySchemes(REFRESH_TOKEN_COOKIE, cookieScheme("rt")));
    }

    // Spring Security는 쿠키를 직접 읽고, Swagger에는 쿠키 이름만 안내한다.
    private SecurityScheme cookieScheme(String cookieName) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(cookieName);
    }
}
