package com.arcguard.firesafety.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FCM 토큰 등록 요청")
public class FcmTokenReq {

    @Schema(description = "프론트에서 발급받은 FCM 푸시 토큰", example = "fcm-token-xxxx")
    private String fcmToken;
}
