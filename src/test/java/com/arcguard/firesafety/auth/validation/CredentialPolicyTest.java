package com.arcguard.firesafety.auth.validation;

import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialPolicyTest {

    private final CredentialPolicy credentialPolicy = new CredentialPolicy();

    @Test
    @DisplayName("이메일 검증: 정상 이메일은 성공한다")
    void validEmailSucceeds() {
        assertThat(credentialPolicy.normalizeEmail("user@example.com")).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("이메일 검증: 앞뒤 공백은 제거하고 소문자로 정규화한다")
    void emailIsTrimmedAndLowercased() {
        assertThat(credentialPolicy.normalizeEmail("  USER@Example.COM  ")).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "userexample.com",
            "@example.com",
            "user@",
            "",
            "   "
    })
    @DisplayName("이메일 검증: 잘못된 이메일은 실패한다")
    void invalidEmailFails(String email) {
        assertThatThrownBy(() -> credentialPolicy.normalizeEmail(email))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT));
    }

    @Test
    @DisplayName("비밀번호 검증: 영문과 숫자가 포함된 8자 이상 비밀번호는 성공한다")
    void validPasswordSucceeds() {
        credentialPolicy.validatePassword("password1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "pass1",
            "12345678",
            "password",
            "pass word1",
            "",
            "abcdefghijklmnopqrstuvwxyz12345"
    })
    @DisplayName("비밀번호 검증: 정책에 맞지 않는 비밀번호는 실패한다")
    void invalidPasswordFails(String password) {
        assertThatThrownBy(() -> credentialPolicy.validatePassword(password))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_PASSWORD_FORMAT));
    }
}
