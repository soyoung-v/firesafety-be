package com.arcguard.firesafety.config.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MyCookieUtilTest {

    private final MyCookieUtil myCookieUtil = new MyCookieUtil();

    @Test
    void HTTP_배포_설정이면_Secure_없이_SameSite만_붙는다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        myCookieUtil.setCookie(response, "at", "token-value", 3600, "/", false, "Lax");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader)
                .contains("at=token-value")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void HTTPS_배포_설정이면_Secure가_붙는다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        myCookieUtil.setCookie(response, "rt", "token-value", 604800, "/api/auth", true, "Lax");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader)
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Path=/api/auth");
    }

    @Test
    void maxAge가_0이면_즉시_만료_쿠키를_만든다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        myCookieUtil.setCookie(response, "at", "", 0, "/", false, "Lax");

        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
