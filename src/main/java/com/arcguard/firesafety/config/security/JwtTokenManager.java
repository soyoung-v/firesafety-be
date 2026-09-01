package com.arcguard.firesafety.config.security;

import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.config.jwt.ConstJwt;
import com.arcguard.firesafety.config.util.MyCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// JWT 생성/쿠키 저장/인증 객체 생성을 한곳에서 조율
@Component
@RequiredArgsConstructor
public class JwtTokenManager {

    private final ConstJwt constJwt;
    private final MyCookieUtil myCookieUtil;
    private final JwtTokenProvider jwtTokenProvider;

    // AT/RT 쿠키 동시 발급
    public void issue(HttpServletResponse response, JwtUser jwtUser) {
        setAccessTokenInCookie(response, jwtUser);
        setRefreshTokenInCookie(response, jwtUser);
    }

    // AT 생성 후 쿠키 저장
    public void setAccessTokenInCookie(HttpServletResponse response, JwtUser jwtUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        setAccessTokenInCookie(response, accessToken);
    }

    // RT 생성 후 쿠키 저장
    public void setRefreshTokenInCookie(HttpServletResponse response, JwtUser jwtUser) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);
        setRefreshTokenInCookie(response, refreshToken);
    }

    // AT 쿠키 저장
    public void setAccessTokenInCookie(HttpServletResponse response, String accessToken) {
        myCookieUtil.setCookie(
                response,
                constJwt.getAccessTokenCookieName(),
                accessToken,
                constJwt.getAccessTokenCookieValiditySeconds(),
                constJwt.getAccessTokenCookiePath(),
                constJwt.isCookieSecure(),
                constJwt.getCookieSameSite()
        );
    }

    // RT 쿠키 저장
    public void setRefreshTokenInCookie(HttpServletResponse response, String refreshToken) {
        myCookieUtil.setCookie(
                response,
                constJwt.getRefreshTokenCookieName(),
                refreshToken,
                constJwt.getRefreshTokenCookieValiditySeconds(),
                constJwt.getRefreshTokenCookiePath(),
                constJwt.isCookieSecure(),
                constJwt.getCookieSameSite()
        );
    }

    // AT 쿠키값 조회
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return myCookieUtil.getValue(request, constJwt.getAccessTokenCookieName());
    }

    // RT 쿠키값 조회
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return myCookieUtil.getValue(request, constJwt.getRefreshTokenCookieName());
    }

    // 인증 쿠키 만료 처리
    public void expireCookies(HttpServletResponse response) {
        myCookieUtil.setCookie(response, constJwt.getAccessTokenCookieName(), "", 0, constJwt.getAccessTokenCookiePath(), constJwt.isCookieSecure(), constJwt.getCookieSameSite());
        myCookieUtil.setCookie(response, constJwt.getRefreshTokenCookieName(), "", 0, constJwt.getRefreshTokenCookiePath(), constJwt.isCookieSecure(), constJwt.getCookieSameSite());
    }

    // Security Authentication 생성
    public Authentication getAuthentication(HttpServletRequest request) {
        String accessToken = getAccessTokenFromCookie(request);
        if (accessToken == null) {
            return null;
        }

        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(accessToken);
        if (jwtUser == null) {
            return null;
        }

        UserPrincipal userPrincipal = new UserPrincipal(jwtUser);
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }
}
