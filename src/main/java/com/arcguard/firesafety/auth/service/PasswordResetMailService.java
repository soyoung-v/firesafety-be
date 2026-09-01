package com.arcguard.firesafety.auth.service;

import com.arcguard.firesafety.auth.config.PasswordResetProperties;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    private static final String LOGO_CID = "arcguard-logo";
    private static final String LOGO_PATH = "static/images/ArcGuard.png";

    // SMTP 제공자가 Gmail/SES로 바뀌어도 JavaMailSender 설정만 교체
    private final JavaMailSender javaMailSender;

    // 비밀번호 재설정 링크와 발신자 정보
    private final PasswordResetProperties passwordResetProperties;

    // 비밀번호 재설정 링크 메일 발송
    public void sendPasswordResetMail(User user, String originalToken) {
        validateMailSettings();

        String resetLink = passwordResetProperties.getBaseUrl() + "?token=" + originalToken;

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // 로고를 인라인 이미지로 첨부하려면 multipart(true) 필요
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(passwordResetProperties.getMailFromAddress(), passwordResetProperties.getMailFromName());
            helper.setTo(user.getEmail());
            helper.setSubject("[ArcGuard] 비밀번호 재설정 안내");
            helper.setText(buildHtmlBody(resetLink, passwordResetProperties.getTokenExpirationMinutes()), true);
            helper.addInline(LOGO_CID, new ClassPathResource(LOGO_PATH));

            javaMailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED);
        }
    }

    // 비밀번호 재설정 메일 HTML 본문
    private String buildHtmlBody(String resetLink, int expirationMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7f9;font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 16px;">
                        <table width="520" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(18,58,74,0.15);">

                          <!-- 헤더 -->
                          <tr>
                            <td style="background:#123a4a;padding:24px 32px;">
                              <table cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="vertical-align:middle;padding-right:10px;">
                                    <img src="cid:%s" alt="ArcGuard" width="44" height="44" style="display:block;border-radius:6px;">
                                  </td>
                                  <td style="vertical-align:middle;">
                                    <span style="color:#fff;font-size:20px;font-weight:700;">아크가드 ArcGuard</span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- 본문 -->
                          <tr>
                            <td style="padding:36px 32px;">
                              <h2 style="margin:0 0 12px;color:#1a202c;font-size:22px;font-weight:700;">비밀번호 재설정 안내</h2>
                              <p style="margin:0 0 8px;color:#555;font-size:14px;line-height:1.7;">
                                아래 버튼을 클릭하여 비밀번호를 재설정하세요.<br>
                                링크는 <strong>%d분</strong> 후 만료됩니다.
                              </p>

                              <!-- 버튼 -->
                              <div style="margin:32px 0;text-align:center;">
                                <a href="%s"
                                   style="display:inline-block;background:#2a91b3;color:#fff;font-size:15px;font-weight:700;padding:14px 36px;border-radius:8px;text-decoration:none;">
                                  비밀번호 재설정하기
                                </a>
                              </div>

                              <!-- 주의 문구 -->
                              <p style="margin:0;color:#999;font-size:12px;line-height:1.6;">
                                본인이 요청하지 않은 경우 이 메일을 무시하세요.<br>
                                링크는 %d분 후 자동으로 만료됩니다.
                              </p>
                            </td>
                          </tr>

                          <!-- 푸터 -->
                          <tr>
                            <td style="background:#f9fafb;padding:20px 32px;border-top:1px solid #e5e7eb;">
                              <p style="margin:0;color:#aaa;font-size:12px;text-align:center;">
                                &#169; 2026 ArcGuard 아크가드. All rights reserved.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(LOGO_CID, expirationMinutes, resetLink, expirationMinutes);
    }

    // SMTP 설정은 환경변수로 주입되어야 함
    private void validateMailSettings() {
        if (!StringUtils.hasText(passwordResetProperties.getBaseUrl())
                || !StringUtils.hasText(passwordResetProperties.getMailFromAddress())
                || !StringUtils.hasText(passwordResetProperties.getMailFromName())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED);
        }
    }
}
