package com.arcguard.firesafety.auth.service;

import com.arcguard.firesafety.auth.config.PasswordResetProperties;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.common.exception.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetMailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private PasswordResetMailService passwordResetMailService;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setBaseUrl("http://localhost:5173/reset-password");
        properties.setTokenExpirationMinutes(30);
        properties.setMailFromAddress("noreply@example.com");
        properties.setMailFromName("아크가드 ArcGuard");

        passwordResetMailService = new PasswordResetMailService(javaMailSender, properties);
    }

    @Test
    @DisplayName("메일 설정: 환경변수 기반 발신자 주소와 이름으로 메일을 발송한다")
    void sendPasswordResetMailUsesConfiguredSender() throws Exception {
        // given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        passwordResetMailService.sendPasswordResetMail(activeUser(), "reset-token");

        // then
        assertThat(mimeMessage.getFrom()[0].toString()).contains("noreply@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("[ArcGuard] 비밀번호 재설정 안내");
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("메일 설정: SMTP 발송 실패 시 비즈니스 예외를 반환한다")
    void sendPasswordResetMailFailureThrowsBusinessException() {
        // given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp failure")).when(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> passwordResetMailService.sendPasswordResetMail(activeUser(), "reset-token"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED));
    }

    private User activeUser() {
        User user = new User();
        user.setEmail("user@example.com");
        return user;
    }
}
