package com.arcguard.firesafety.config.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// at/rt HttpOnly Cookie 생성/조회
@Component
public class MyCookieUtil {

    // jakarta.servlet.http.Cookie는 SameSite 속성을 지원하지 않아 ResponseCookie로 Set-Cookie 헤더를 직접 구성
    public void setCookie(HttpServletResponse response, String key, String value, int maxAge, String path, boolean secure, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(key, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(Duration.ofSeconds(maxAge))
                .path(path != null ? path : "/")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Cookie 값 조회
    public String getValue(HttpServletRequest request, String key) {
        Cookie cookie = getCookie(request, key);
        return cookie == null ? null : cookie.getValue();
    }

    // Cookie 객체 조회
    public Cookie getCookie(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie;
            }
        }
        return null;
    }
}
