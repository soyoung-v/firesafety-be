package com.arcguard.firesafety.alert.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.arcguard.firesafety.alert.event.AlertNotificationEvent;
import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.config.firebase.FirebaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final AuthMapper authMapper;
    private final FirebaseProperties firebaseProperties;

    // 경보 푸시 발송
    // Firebase 설정이 없으면 local/test로 보고 발송을 건너뛴다.
    public void sendAlert(AlertNotificationEvent event) {
        if (!firebaseProperties.isReady() || event.getSiteId() == null) {
            return;
        }

        List<String> tokens = authMapper.findFcmTokensForAlertSite(event.getSiteId());
        if (CollectionUtils.isEmpty(tokens)) {
            return;
        }

        try {
            // notification 필드를 같이 보내면 브라우저가 알림을 자동으로 한 번 띄우고,
            // 서비스워커(onBackgroundMessage)가 또 한 번 띄워서 웹푸시가 중복으로 뜬다.
            // data-only 메시지로만 보내서 서비스워커가 정확히 한 번만 표시하게 한다.
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .putData("title", buildTitle(event))
                    .putData("body", buildBody(event))
                    .putData("eventType", event.getEventType())
                    .putData("alertId", String.valueOf(event.getAlertId()))
                    .putData("panelId", String.valueOf(event.getPanelId()))
                    .putData("severity", event.getSeverity().name())
                    .build();

            BatchResponse response = getFirebaseMessaging().sendEachForMulticast(message);
            // 발송이 시도됐는지/성공했는지를 실패 여부와 무관하게 남겨서, 조용히 성공한 건지
            // 애초에 이 지점까지 도달을 못 한 건지(리스너 앞단 예외 등) 구분할 수 있게 한다.
            log.info("FCM 경보 발송 완료 - alertId={}, successCount={}, failureCount={}",
                    event.getAlertId(), response.getSuccessCount(), response.getFailureCount());
            if (response.getFailureCount() > 0) {
                handleFailures(tokens, response, event);
            }
        } catch (RuntimeException | IOException | FirebaseMessagingException e) {
            // 푸시 실패가 경보 저장 흐름을 깨면 안 되므로 로그만 남긴다.
            log.warn("FCM 경보 발송 실패 - alertId={}, message={}", event.getAlertId(), e.getMessage());
        }
    }

    // 실패한 토큰별 사유를 로그로 남기고, 더 이상 유효하지 않은 토큰(UNREGISTERED)은 DB에서 정리한다.
    private void handleFailures(List<String> tokens, BatchResponse response, AlertNotificationEvent event) {
        List<SendResponse> results = response.getResponses();
        List<String> deadTokens = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            SendResponse result = results.get(i);
            if (result.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException exception = result.getException();
            MessagingErrorCode errorCode = exception.getMessagingErrorCode();
            // errorCode(FCM 전용 코드)가 null이면 인증/네트워크 등 더 일반적인 오류라 메시지까지 남겨야 원인 파악 가능
            log.warn("FCM 개별 발송 실패 - alertId={}, errorCode={}, generalErrorCode={}, message={}",
                    event.getAlertId(), errorCode, exception.getErrorCode(), exception.getMessage());
            if (errorCode == MessagingErrorCode.UNREGISTERED) {
                deadTokens.add(tokens.get(i));
            }
        }

        if (!deadTokens.isEmpty()) {
            authMapper.deleteFcmTokens(deadTokens);
        }
    }

    // Firebase Admin SDK 인스턴스 준비
    private FirebaseMessaging getFirebaseMessaging() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try (FileInputStream inputStream = new FileInputStream(firebaseProperties.getCredentialsPath())) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }
        return FirebaseMessaging.getInstance();
    }

    // 사용자에게 보여줄 짧은 푸시 본문 생성
    private String buildTitle(AlertNotificationEvent event) {
        return "[%s] ArcGuard 경보 발생".formatted(event.getSeverity().getLabel());
    }

    private String buildBody(AlertNotificationEvent event) {
        return event.getSource().getLabel() + " " + event.getType().getLabel() + " 경보가 발생했습니다";
    }
}
