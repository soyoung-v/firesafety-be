package com.arcguard.firesafety.sensor.dto.req;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "레거시 프로토콜 센서 장비 데이터 수신 쿼리 파라미터")
public class SensorFrameIngestReq {

    @Parameter(description = "장비번호 5자리", example = "00001", required = true)
    private String m_no;

    @Parameter(description = "운영모드 1자리", example = "0", required = true)
    private String mode;

    @Parameter(description = "전압값 3자리, 단위 V", example = "230", required = true)
    private String volt;

    @Parameter(description = "1회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am1;

    @Parameter(description = "1회로 카운터 4자리", example = "0000", required = true)
    private String count1;

    @Parameter(description = "2회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am2;

    @Parameter(description = "2회로 카운터 4자리", example = "0000", required = true)
    private String count2;

    @Parameter(description = "3회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am3;

    @Parameter(description = "3회로 카운터 4자리", example = "0000", required = true)
    private String count3;

    @Parameter(description = "4회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am4;

    @Parameter(description = "4회로 카운터 4자리", example = "0000", required = true)
    private String count4;

    @Parameter(description = "5회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am5;

    @Parameter(description = "5회로 카운터 4자리", example = "0000", required = true)
    private String count5;

    @Parameter(description = "6회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am6;

    @Parameter(description = "6회로 카운터 4자리", example = "0000", required = true)
    private String count6;

    @Parameter(description = "7회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am7;

    @Parameter(description = "7회로 카운터 4자리", example = "0000", required = true)
    private String count7;

    @Parameter(description = "8회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am8;

    @Parameter(description = "8회로 카운터 4자리", example = "0000", required = true)
    private String count8;

    @Parameter(description = "9회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am9;

    @Parameter(description = "9회로 카운터 4자리", example = "0000", required = true)
    private String count9;

    @Parameter(description = "10회로 전류값 3자리, 0.1A 단위", example = "000", required = true)
    private String am10;

    @Parameter(description = "10회로 카운터 4자리", example = "0000", required = true)
    private String count10;

    @Parameter(description = "HCT 카운터 4자리", example = "0005", required = true)
    private String hct_count;

    @Parameter(description = "누설전류값 2자리, 단위 mA", example = "00", required = true)
    private String s_circuit;

    @Parameter(description = "온도값 3자리, 0.1도 단위", example = "272", required = true)
    private String tem;

    @Parameter(description = "습도값 3자리, 0.1% 단위", example = "484", required = true)
    private String humi;

    @Parameter(description = "불꽃센서 원시값 4자리", example = "3923", required = true)
    private String fire;

    @Parameter(description = "가스센서 원시값 4자리", example = "2212", required = true)
    private String gas;

    @Parameter(description = "각종 에러값 16진수 8자리. byte0~1은 ARC, byte2는 ERROR, byte3은 ALARM", example = "18060000", required = true)
    private String aerror;

    @Parameter(description = "도어 상태 1자리. 0=닫힘, 1=열림", example = "1", required = true)
    private String door;

    @Parameter(description = "전체전류값 2자리, 단위 A", example = "00", required = true)
    private String total_circuit;

    @Parameter(description = "전체전력값 5자리, 단위 W", example = "00000", required = true)
    private String e_energy;
}
