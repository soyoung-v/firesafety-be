package com.arcguard.firesafety.auth.service;

import com.arcguard.firesafety.auth.config.PasswordResetProperties;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PasswordResetRateLimiter {

    private final PasswordResetProperties properties;
    private final Map<String, Deque<LocalDateTime>> requestTimes = new ConcurrentHashMap<>();

    // 비밀번호 재설정 메일 반복 요청 제한
    public void check(String email, String requestIp) {
        String key = buildKey(email, requestIp);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(properties.getRateLimitWindowMinutes());

        Deque<LocalDateTime> times = requestTimes.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (times) {
            // 제한 시간보다 오래된 요청 기록은 제거
            while (!times.isEmpty() && times.peekFirst().isBefore(windowStart)) {
                times.removeFirst();
            }

            if (times.size() >= properties.getRateLimitMaxCount()) {
                throw new BusinessException(AuthErrorCode.PASSWORD_RESET_RATE_LIMIT);
            }

            times.addLast(now);
        }
    }

    // 이메일과 IP를 함께 사용해 같은 사용자의 반복 요청만 제한
    private String buildKey(String email, String requestIp) {
        return email.toLowerCase(Locale.ROOT) + "|" + requestIp;
    }
}
