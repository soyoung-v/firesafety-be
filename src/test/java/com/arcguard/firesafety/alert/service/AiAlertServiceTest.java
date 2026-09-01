package com.arcguard.firesafety.alert.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAlertServiceTest {

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private AlertNotificationPublisher alertNotificationPublisher;

    private AiAlertService aiAlertService;

    @BeforeEach
    void setUp() {
        aiAlertService = new AiAlertService(alertMapper, alertNotificationPublisher);
    }

    @Test
    @DisplayName("FR-02-01: AI ARC 판정이면 AI 소스 경보를 생성한다")
    void createAiArcAlert() {
        // given
        when(alertMapper.existsUnresolvedCircuitAlert(20L, "AI", "ARC", "CAUTION")).thenReturn(false);

        // when
        aiAlertService.createArcAlert(10L, 20L, 30L);

        // then
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertMapper).insertAlert(alertCaptor.capture());

        Alert alert = alertCaptor.getValue();
        assertThat(alert.getPanelId()).isEqualTo(10L);
        assertThat(alert.getCircuitId()).isEqualTo(20L);
        assertThat(alert.getResultId()).isEqualTo(30L);
        assertThat(alert.getSource()).isEqualTo(AlertSource.AI);
        assertThat(alert.getType()).isEqualTo(AlertType.ARC);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
        verify(alertNotificationPublisher).publishCreated(alert);
    }

    @Test
    @DisplayName("FR-02-01: 같은 회로의 미조치 AI ARC 경보가 있으면 중복 생성하지 않는다")
    void skipDuplicatedAiArcAlert() {
        // given
        when(alertMapper.existsUnresolvedCircuitAlert(20L, "AI", "ARC", "CAUTION")).thenReturn(true);

        // when
        aiAlertService.createArcAlert(10L, 20L, 30L);

        // then
        verify(alertMapper, never()).insertAlert(org.mockito.Mockito.any());
        verify(alertNotificationPublisher, never()).publishCreated(org.mockito.Mockito.any(Alert.class));
    }
}
