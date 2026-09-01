package com.arcguard.firesafety.config.security;

import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 요청당 1회 실행되는 JWT 인증 필터
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;

    // AT 쿠키 인증 처리
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String accessToken = jwtTokenManager.getAccessTokenFromCookie(request);
        Authentication authentication = jwtTokenManager.getAuthentication(request);

        if (authentication != null) {
            // 이후 컨트롤러와 서비스에서 인증 사용자 컨텍스트 접근
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 인증 성공 - userId: {}", authentication.getName());
        } else if (accessToken != null) {
            // at 쿠키는 있었지만 검증 실패한 경우 EntryPoint가 공통 401 응답으로 바꿀 수 있게 표시한다.
            request.setAttribute(
                    JwtAuthenticationEntryPoint.AUTH_EXCEPTION_ATTRIBUTE,
                    new BusinessException(AuthErrorCode.EXPIRED_AUTH)
            );
        }

        filterChain.doFilter(request, response);
    }
}
