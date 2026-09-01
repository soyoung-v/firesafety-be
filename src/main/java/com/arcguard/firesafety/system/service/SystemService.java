package com.arcguard.firesafety.system.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.system.dto.req.SystemReleaseCreateReq;
import com.arcguard.firesafety.system.dto.res.SystemReleasePageRes;
import com.arcguard.firesafety.system.dto.res.SystemVersionHistoryRes;
import com.arcguard.firesafety.system.dto.res.SystemVersionRes;
import com.arcguard.firesafety.system.mapper.SystemMapper;
import com.arcguard.firesafety.system.model.SystemReleaseHistory;
import com.arcguard.firesafety.system.model.SystemReleaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SystemService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 11;
    private static final int MAX_SIZE = 100;

    // 유의적 버전(Semantic Versioning): Major.Minor.Patch만 허용, 프리릴리즈/빌드 메타데이터는 받지 않는다
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private final SystemMapper systemMapper;

    // SW 버전 정보 요약 — 등록된 이력 중 최신 소프트웨어/AI 모델 버전만 보여준다
    @Transactional(readOnly = true)
    public SystemVersionRes getVersion() {
        SystemReleaseHistory latestSoftware = systemMapper.findLatestByType(SystemReleaseType.SOFTWARE);
        SystemReleaseHistory latestModel = systemMapper.findLatestByType(SystemReleaseType.MODEL);

        return new SystemVersionRes(
                latestSoftware != null ? latestSoftware.getVersion() : null,
                latestSoftware != null ? latestSoftware.getReleasedAt() : null,
                latestModel != null ? latestModel.getVersion() : null
        );
    }

    // 업데이트 이력 페이지 조회(소프트웨어/AI 모델 이력을 최신순으로 함께 보여줌)
    @Transactional(readOnly = true)
    public SystemReleasePageRes getReleases(Integer pageParam, Integer sizeParam) {
        int page = resolvePage(pageParam);
        int size = resolveSize(sizeParam);

        List<SystemVersionHistoryRes> content = systemMapper.findReleases(size, page * size).stream()
                .map(SystemVersionHistoryRes::from)
                .toList();
        long totalElements = systemMapper.countReleases();

        return new SystemReleasePageRes(content, totalElements, page, size);
    }

    // 업데이트 이력 등록 — SUPER_ADMIN 전용, 등록 모달에서 직접 입력
    @Transactional
    public void createRelease(SystemReleaseCreateReq req) {
        requireSuperAdmin(getCurrentUser());
        validateVersion(req);

        SystemReleaseHistory release = new SystemReleaseHistory();
        release.setVersion(req.getVersion());
        release.setType(req.getType());
        release.setDescription(req.getDescription());
        release.setUpdatedBy(req.getUpdatedBy());
        release.setReleasedAt(resolveReleasedAt(req.getReleasedAt()));
        systemMapper.insertRelease(release);
    }

    // 소프트웨어 버전은 유의적 버전(Major.Minor.Patch) 형식만 허용 — AI 모델 버전은 형식 강제하지 않음
    private void validateVersion(SystemReleaseCreateReq req) {
        if (req.getType() == SystemReleaseType.SOFTWARE && !SEMVER_PATTERN.matcher(req.getVersion()).matches()) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
        }
    }

    private LocalDateTime resolveReleasedAt(LocalDate releasedAt) {
        return releasedAt == null ? LocalDateTime.now() : releasedAt.atStartOfDay();
    }

    private int resolvePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return size;
    }

    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }
}
