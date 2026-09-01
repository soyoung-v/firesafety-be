package com.arcguard.firesafety.auth.validation;

import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CredentialPolicy {

    private static final int EMAIL_MAX_LENGTH = 100;
    private static final int PASSWORD_MAX_LENGTH = 30;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$");

    // 이메일은 앞뒤 공백 제거 후 소문자로 통일해서 저장/조회
    public String normalizeEmail(String email) {
        validateEmail(email);
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 관리자 생성/수정, 로그인, 비밀번호 재설정 요청에서 같은 이메일 형식을 사용
    public void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }

        String trimmedEmail = email.trim();
        if (trimmedEmail.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    // 초기 비밀번호와 비밀번호 재설정은 같은 정책을 사용
    public void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() > PASSWORD_MAX_LENGTH || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }
}
