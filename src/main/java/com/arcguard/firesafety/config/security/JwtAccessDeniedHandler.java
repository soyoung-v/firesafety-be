package com.arcguard.firesafety.config.security;

import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

// 인가는 됐지만 권한이 부족한 403 응답도 GlobalExceptionHandler의 ResultResponse로 통일한다.
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        handlerExceptionResolver.resolveException(
                request,
                response,
                null,
                new BusinessException(AuthErrorCode.FORBIDDEN_ROLE)
        );
    }
}
