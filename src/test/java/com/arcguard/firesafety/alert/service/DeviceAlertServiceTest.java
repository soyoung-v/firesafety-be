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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceAlertServiceTest {

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private AlertNotificationPublisher alertNotificationPublisher;

    private DeviceAlertService deviceAlertService;

    @BeforeEach
    void setUp() {
        deviceAlertService = new DeviceAlertService(alertMapper, alertNotificationPublisher);
    }

    @Test
    @DisplayName("FR-01-03: aerror ARC/ERROR/ALARM bit를 DEVICE 경보로 저장한다")
    void createDeviceAlertsFromAerrorBits() {
        // given
        Map<Integer, Long> circuitIdsByChannelNo = Map.of(
                4, 104L,
                5, 105L
        );

        // when
        deviceAlertService.createDeviceAlerts(10L, "18060727", circuitIdsByChannelNo);

        // then
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertMapper, org.mockito.Mockito.times(7)).insertAlert(alertCaptor.capture());
        verify(alertNotificationPublisher, org.mockito.Mockito.times(7)).publishCreated(org.mockito.Mockito.any(Alert.class));

        assertThat(alertCaptor.getAllValues())
                .extracting(Alert::getSource)
                .containsOnly(AlertSource.DEVICE);
        assertThat(alertCaptor.getAllValues())
                .extracting(Alert::getStatus)
                .containsOnly(AlertStatus.UNCONFIRMED);
        assertThat(alertCaptor.getAllValues())
                .extracting(Alert::getType)
                .containsExactly(
                        AlertType.ARC,
                        AlertType.ARC,
                        AlertType.DEVICE_ERROR,
                        AlertType.LEAKAGE,
                        AlertType.OVERHEAT,
                        AlertType.HUMIDITY,
                        AlertType.DOOR_OPEN
                );
        assertThat(alertCaptor.getAllValues().get(0).getCircuitId()).isEqualTo(104L);
        assertThat(alertCaptor.getAllValues().get(1).getCircuitId()).isEqualTo(105L);
    }

    @Test
    @DisplayName("FR-01-03: aerror가 정상이면 DEVICE 경보를 만들지 않는다")
    void normalAerrorDoesNotCreateAlert() {
        // when
        deviceAlertService.createDeviceAlerts(10L, "00000000", Map.of());

        // then
        verify(alertMapper, never()).insertAlert(org.mockito.Mockito.any());
        verify(alertNotificationPublisher, never()).publishCreated(org.mockito.Mockito.any(Alert.class));
    }
}
