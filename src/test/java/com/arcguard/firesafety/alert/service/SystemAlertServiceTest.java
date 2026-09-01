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
class SystemAlertServiceTest {

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private AlertNotificationPublisher alertNotificationPublisher;

    private SystemAlertService systemAlertService;

    @BeforeEach
    void setUp() {
        systemAlertService = new SystemAlertService(alertMapper, alertNotificationPublisher);
    }

    @Test
    @DisplayName("FR-01-04: 통신두절 SYSTEM 경보를 생성한다")
    void createCommunicationLostAlert() {
        // given
        when(alertMapper.existsUnresolvedAlert(10L, "SYSTEM", "COMM_LOST", "RISK")).thenReturn(false);

        // when
        systemAlertService.createCommunicationLostAlert(10L);

        // then
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertMapper).insertAlert(alertCaptor.capture());

        Alert alert = alertCaptor.getValue();
        assertThat(alert.getPanelId()).isEqualTo(10L);
        assertThat(alert.getSource()).isEqualTo(AlertSource.SYSTEM);
        assertThat(alert.getType()).isEqualTo(AlertType.COMM_LOST);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
        verify(alertNotificationPublisher).publishCreated(alert);
    }

    @Test
    @DisplayName("FR-01-04: 미조치 통신두절 경보가 있으면 중복 생성하지 않는다")
    void skipDuplicatedCommunicationLostAlert() {
        // given
        when(alertMapper.existsUnresolvedAlert(10L, "SYSTEM", "COMM_LOST", "RISK")).thenReturn(true);

        // when
        systemAlertService.createCommunicationLostAlert(10L);

        // then
        verify(alertMapper, never()).insertAlert(org.mockito.Mockito.any());
        verify(alertNotificationPublisher, never()).publishCreated(org.mockito.Mockito.any(Alert.class));
    }
}
