package com.arcguard.firesafety.system.mapper;

import com.arcguard.firesafety.system.model.SystemReleaseHistory;
import com.arcguard.firesafety.system.model.SystemReleaseType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemMapper {

    // 릴리즈 이력 등록 — SUPER_ADMIN이 SW 버전 정보 화면의 등록 모달에서 직접 입력
    void insertRelease(SystemReleaseHistory release);

    // 특정 구분(SOFTWARE/MODEL)의 최신 릴리즈 1건 — 없으면 null
    SystemReleaseHistory findLatestByType(@Param("type") SystemReleaseType type);

    // 릴리즈 이력 최신순 페이지 조회
    List<SystemReleaseHistory> findReleases(@Param("limit") int limit, @Param("offset") int offset);

    // 릴리즈 이력 전체 개수
    long countReleases();
}
