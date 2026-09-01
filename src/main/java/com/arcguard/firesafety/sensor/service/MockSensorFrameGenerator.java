package com.arcguard.firesafety.sensor.service;

import com.arcguard.firesafety.facility.model.Panel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 데모용 가짜 센서 프레임 생성. /m_noUpload.php와 같은 자리수 규격으로 값 생성.
// 분전반별로 정상 -> 주의 -> 위험 순으로 순환하는 상태머신이라, 데모에서 세 상태를 다 확인할 수 있다.
// 하드웨어 연결되면 이 클래스는 안 씀.
@Component
public class MockSensorFrameGenerator {

    private static final int MAX_CHANNEL_COUNT = 10;
    // ALARM bit 종류 수, 순서는 DeviceAlertService와 동일 (누전/과열/습도/가스/불꽃/문열림/과전류)
    private static final int ALARM_BIT_COUNT = 7;
    private static final int ALARM_BIT_LEAKAGE = 0;
    private static final int ALARM_BIT_OVERHEAT = 1;
    private static final int ALARM_BIT_HUMIDITY = 2;
    private static final int ALARM_BIT_GAS = 3;
    private static final int ALARM_BIT_FIRE = 4;
    private static final int ALARM_BIT_DOOR_OPEN = 5;
    private static final int ALARM_BIT_OVERCURRENT = 6;

    // panel의 threshold가 비어있을 때만 쓰는 기본값 (PanelService 기본값과 동일)
    private static final int DEFAULT_TEMP_THRESHOLD_TENTHS = 800;
    private static final int DEFAULT_HUMIDITY_THRESHOLD_TENTHS = 800;
    private static final int DEFAULT_GAS_THRESHOLD = 5000;
    private static final int DEFAULT_FIRE_THRESHOLD = 5000;
    private static final int DEFAULT_LEAK_MA_THRESHOLD = 20;
    private static final int DEFAULT_OVERCURRENT_THRESHOLD = 30;

    // tem/humi는 3자리, gas/fire는 4자리, s_circuit/total_circuit은 2자리 고정폭이라 값이 자릿수를 넘지 않게 상한을 둔다
    private static final int MAX_2_DIGIT = 99;
    private static final int MAX_3_DIGIT = 999;
    private static final int MAX_4_DIGIT = 9999;

    private enum Phase { NORMAL, CAUTION, RISK }

    private enum CautionMetric { TEMPERATURE, HUMIDITY, GAS, FIRE }

    // 상태 지속 주기(tick). 주의는 백엔드의 30초 지속판정(PanelStatusAggregationService)을 확실히 넘기게 넉넉히 잡음
    private static final int NORMAL_TICKS = 6;
    private static final int CAUTION_TICKS = 10;
    private static final int RISK_TICKS = 4;

    private static class PanelMockState {
        Phase phase = Phase.NORMAL;
        int remainingTicks;
        CautionMetric cautionMetric;
    }

    // 분전반별 순환 상태 보관 (스케줄러가 순차 호출하지만 안전하게 ConcurrentHashMap 사용)
    private final Map<Long, PanelMockState> panelStates = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Map<String, String> generate(Panel panel) {
        int circuitCount = resolveCircuitCount(panel);
        Phase phase = advancePhase(panel);
        CautionMetric cautionMetric = phase == Phase.CAUTION ? panelStates.get(panel.getPanelId()).cautionMetric : null;

        // 정상이면 -1 유지. 위험 구간에서만 아크/알람 중 하나를 무작위로 고름
        int arcChannel = -1;
        int alarmBitIndex = -1;
        if (phase == Phase.RISK) {
            if (random.nextBoolean()) {
                arcChannel = 1 + random.nextInt(circuitCount);
            } else {
                alarmBitIndex = random.nextInt(ALARM_BIT_COUNT);
            }
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("m_no", panel.getMNo());
        params.put("mode", "0");
        params.put("volt", pad(220 + random.nextInt(10), 3));
        params.put("hct_count", pad(random.nextInt(1000), 4));
        params.put("s_circuit", pad(alarmBitIndex == ALARM_BIT_LEAKAGE
                ? boundedThreshold(panel.getLeakMaThreshold(), DEFAULT_LEAK_MA_THRESHOLD, MAX_2_DIGIT) + 1 + random.nextInt(10)
                : random.nextInt(5), 2));
        params.put("tem", pad(resolveTem(panel, cautionMetric, alarmBitIndex), 3));
        params.put("humi", pad(resolveHumi(panel, cautionMetric, alarmBitIndex), 3));
        params.put("fire", pad(alarmBitIndex == ALARM_BIT_FIRE
                ? 6000 + random.nextInt(3000)
                : resolveFireOrGas(panel.getFireThreshold(), DEFAULT_FIRE_THRESHOLD, cautionMetric == CautionMetric.FIRE), 4));
        params.put("gas", pad(alarmBitIndex == ALARM_BIT_GAS
                ? 6000 + random.nextInt(3000)
                : resolveFireOrGas(panel.getGasThreshold(), DEFAULT_GAS_THRESHOLD, cautionMetric == CautionMetric.GAS), 4));
        params.put("door", alarmBitIndex == ALARM_BIT_DOOR_OPEN ? "1" : "0");
        params.put("total_circuit", pad(alarmBitIndex == ALARM_BIT_OVERCURRENT
                ? boundedThreshold(panel.getOvercurrentThreshold(), DEFAULT_OVERCURRENT_THRESHOLD, MAX_2_DIGIT) + 1 + random.nextInt(10)
                : random.nextInt(20), 2));
        params.put("e_energy", pad(random.nextInt(20000), 5));
        params.put("aerror", buildAerror(arcChannel, alarmBitIndex));

        for (int channelNo = 1; channelNo <= MAX_CHANNEL_COUNT; channelNo++) {
            boolean isArcChannel = channelNo == arcChannel;
            params.put("am" + channelNo, pad(isArcChannel ? 150 + random.nextInt(150) : random.nextInt(50), 3));
            params.put("count" + channelNo, pad(isArcChannel ? 1 + random.nextInt(20) : 0, 4));
        }

        return params;
    }

    // 분전반별 상태 순환: 정상 -> 주의(임계치만 초과, 알람비트 없음) -> 위험(알람/아크) -> 정상 ...
    // 분전반마다 시작 지점을 다르게 둬서 전체가 한번에 안 바뀌게 함
    private Phase advancePhase(Panel panel) {
        PanelMockState state = panelStates.computeIfAbsent(panel.getPanelId(), id -> {
            PanelMockState s = new PanelMockState();
            s.remainingTicks = 1 + random.nextInt(NORMAL_TICKS);
            return s;
        });

        state.remainingTicks--;
        if (state.remainingTicks <= 0) {
            state.phase = nextPhase(state.phase);
            state.remainingTicks = ticksFor(state.phase);
            if (state.phase == Phase.CAUTION) {
                state.cautionMetric = CautionMetric.values()[random.nextInt(CautionMetric.values().length)];
            }
        }
        return state.phase;
    }

    private Phase nextPhase(Phase phase) {
        return switch (phase) {
            case NORMAL -> Phase.CAUTION;
            case CAUTION -> Phase.RISK;
            case RISK -> Phase.NORMAL;
        };
    }

    private int ticksFor(Phase phase) {
        return switch (phase) {
            case NORMAL -> NORMAL_TICKS;
            case CAUTION -> CAUTION_TICKS;
            case RISK -> RISK_TICKS;
        };
    }

    // CAUTION 단계(수치만 기준 초과)뿐 아니라 RISK 단계에서 이 항목이 알람비트로 선택된 경우에도
    // 실제 수치를 같이 올려야 화면 카드가 흰색으로 안 남고 원인이 눈에 보인다.
    private int resolveTem(Panel panel, CautionMetric cautionMetric, int alarmBitIndex) {
        if (cautionMetric == CautionMetric.TEMPERATURE || alarmBitIndex == ALARM_BIT_OVERHEAT) {
            int thresholdTenths = panel.getTempThreshold() != null
                    ? panel.getTempThreshold().multiply(BigDecimal.TEN).intValue()
                    : DEFAULT_TEMP_THRESHOLD_TENTHS;
            return Math.min(MAX_3_DIGIT, thresholdTenths + 10 + random.nextInt(100));
        }
        return 200 + random.nextInt(100);
    }

    private int resolveHumi(Panel panel, CautionMetric cautionMetric, int alarmBitIndex) {
        if (cautionMetric == CautionMetric.HUMIDITY || alarmBitIndex == ALARM_BIT_HUMIDITY) {
            int thresholdTenths = panel.getHumidityThreshold() != null
                    ? panel.getHumidityThreshold().multiply(BigDecimal.TEN).intValue()
                    : DEFAULT_HUMIDITY_THRESHOLD_TENTHS;
            return Math.min(MAX_3_DIGIT, thresholdTenths + 10 + random.nextInt(100));
        }
        return 400 + random.nextInt(200);
    }

    private int resolveFireOrGas(Integer threshold, int defaultThreshold, boolean isCautionTarget) {
        if (isCautionTarget) {
            int base = threshold != null ? threshold : defaultThreshold;
            return Math.min(MAX_4_DIGIT, base + 200 + random.nextInt(800));
        }
        return 100 + random.nextInt(900);
    }

    // 임계값(BigDecimal, 없으면 기본값)을 정수로 변환하고 필드 자릿수 상한을 넘지 않게 자른다
    private int boundedThreshold(BigDecimal threshold, int defaultValue, int maxValue) {
        int base = threshold != null ? threshold.intValue() : defaultValue;
        return Math.min(base, maxValue - 11);
    }

    // circuit_count 값 이상하면 최대값으로 보정
    private int resolveCircuitCount(Panel panel) {
        Integer circuitCount = panel.getCircuitCount();
        if (circuitCount == null || circuitCount < 1 || circuitCount > MAX_CHANNEL_COUNT) {
            return MAX_CHANNEL_COUNT;
        }
        return circuitCount;
    }

    // byte0/1=ARC(회로별), byte2=ERROR(안 씀), byte3=ALARM
    private String buildAerror(int arcChannel, int alarmBitIndex) {
        int byte0 = 0;
        int byte1 = 0;
        int byte3 = 0;
        if (arcChannel > 0) {
            if (arcChannel <= 5) {
                byte0 |= 1 << (arcChannel - 1);
            } else {
                byte1 |= 1 << (arcChannel - 6);
            }
        }
        if (alarmBitIndex >= 0) {
            byte3 |= 1 << alarmBitIndex;
        }
        return String.format("%02X%02X%02X%02X", byte0, byte1, 0, byte3);
    }

    private String pad(int value, int width) {
        return String.format("%0" + width + "d", value);
    }
}
