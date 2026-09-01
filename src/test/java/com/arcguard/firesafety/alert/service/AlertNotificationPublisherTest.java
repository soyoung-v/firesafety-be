package com.arcguard.firesafety.alert.service;

import com.arcguard.firesafety.alert.event.AlertNotificationEvent;
import com.arcguard.firesafety.alert.mapper.AlertMapper;
import com.arcguard.firesafety.alert.model.Alert;
import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertNotificationPublisherTest {

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private AlertNotificationPublisher alertNotificationPublisher;

    @BeforeEach
    void setUp() {
        alertNotificationPublisher = new AlertNotificationPublisher(alertMapper, applicationEventPublisher);
    }

    @Test
    @DisplayName("FR-04-02: 경보 생성 후 알림 이벤트를 발행한다")
    void publishCreatedAlertEvent() {
        // given
        when(alertMapper.findSiteIdByPanelId(10L)).thenReturn(3L);

        // when
        alertNotificationPublisher.publishCreated(alert());

        // then
        ArgumentCaptor<AlertNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlertNotificationEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        AlertNotificationEvent event = eventCaptor.getValue();
        assertThat(event.getAlertId()).isEqualTo(100L);
        assertThat(event.getSiteId()).isEqualTo(3L);
        assertThat(event.getEventType()).isEqualTo("ALERT_CREATED");
    }

    private Alert alert() {
        Alert alert = new Alert();
        alert.setAlertId(100L);
        alert.setPanelId(10L);
        alert.setCircuitId(20L);
        alert.setSource(AlertSource.DEVICE);
        alert.setType(AlertType.ARC);
        alert.setStatus(AlertStatus.UNCONFIRMED);
        return alert;
    }
}
