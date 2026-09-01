package com.arcguard.firesafety.config.security;

import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

// 인증 실패(401)를 Spring Security 기본 응답이 아니라 GlobalExceptionHandler의 ResultResponse로 변환한다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String AUTH_EXCEPTION_ATTRIBUTE = "authException";

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) {
        Exception exception = resolveException(request);
        handlerExceptionResolver.resolveException(request, response, null, exception);
    }

    // 필터에서 구체적인 인증 실패를 담아두지 않았다면 공통 만료 응답으로 처리한다.
    private Exception resolveException(HttpServletRequest request) {
        Object exception = request.getAttribute(AUTH_EXCEPTION_ATTRIBUTE);
        if (exception instanceof Exception resolvedException) {
            return resolvedException;
        }
        return new BusinessException(AuthErrorCode.EXPIRED_AUTH);
    }
}
