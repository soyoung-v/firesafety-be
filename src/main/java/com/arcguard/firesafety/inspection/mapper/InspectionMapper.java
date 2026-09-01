package com.arcguard.firesafety.inspection.mapper;

import com.arcguard.firesafety.inspection.dto.res.InspectionExportRowRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionHistoryRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionItemRes;
import com.arcguard.firesafety.inspection.dto.res.InspectionResultItemRes;
import com.arcguard.firesafety.inspection.model.InspectionItem;
import com.arcguard.firesafety.inspection.model.InspectionResult;
import com.arcguard.firesafety.inspection.model.InspectionResultItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 설비 점검 관리(REQ-511/512)용 MyBatis Mapper
@Mapper
public interface InspectionMapper {

    // 점검 항목 등록 (현장 카탈로그)
    void insertInspectionItem(InspectionItem item);

    // 점검 항목 수정 (현장 카탈로그)
    void updateInspectionItem(InspectionItem item);

    // 점검 항목 삭제 (현장 카탈로그)
    void deleteInspectionItem(@Param("itemId") Long itemId);

    // 점검 항목이 어느 분전반에든 적용돼 있는지 확인 (삭제 가능 여부 판단용)
    boolean existsPanelInspectionItemByItemId(@Param("itemId") Long itemId);

    // 점검 항목이 점검 결과에 이미 쓰인 적 있는지 확인 (삭제 가능 여부 판단용)
    boolean existsInspectionResultItemByItemId(@Param("itemId") Long itemId);

    // 현장의 점검 항목 카탈로그 전체 조회
    List<InspectionItemRes> findInspectionItemsBySiteId(@Param("siteId") Long siteId);

    // 분전반에 적용된 점검 항목 목록 조회 (panel_inspection_item으로 카탈로그를 조인)
    List<InspectionItemRes> findInspectionItemsByPanelId(@Param("panelId") Long panelId);

    // 점검 항목이 해당 현장 카탈로그 소속인지 확인 (분전반 적용 시 다른 현장 항목 섞이는 것 방지)
    boolean existsInspectionItemInSite(@Param("itemId") Long itemId, @Param("siteId") Long siteId);

    // 점검 항목이 해당 분전반에 적용된 상태인지 확인 (체크리스트 저장 시 유효성 검증용)
    boolean existsInspectionItem(@Param("itemId") Long itemId, @Param("panelId") Long panelId);

    // 분전반에 적용된 점검 항목 전체 삭제 (전체교체의 delete 단계)
    void deletePanelInspectionItems(@Param("panelId") Long panelId);

    // 분전반에 점검 항목 일괄 적용 (전체교체의 insert 단계)
    void insertPanelInspectionItems(@Param("panelId") Long panelId, @Param("itemIds") List<Long> itemIds);

    // 점검 실행 1건 등록
    void insertInspectionResult(InspectionResult inspectionResult);

    // 점검 실행 1건에 딸린 항목별 결과 등록
    void insertInspectionResultItem(InspectionResultItem inspectionResultItem);

    // 분전반의 점검 이력 목록 조회 (항목별 결과는 별도 조회해서 조립)
    List<InspectionHistoryRes> findInspectionHistory(@Param("panelId") Long panelId,
                                                     @Param("fromAt") LocalDateTime fromAt,
                                                     @Param("toAt") LocalDateTime toAt,
                                                     @Param("size") int size,
                                                     @Param("offset") int offset);

    // 점검 이력 전체 개수 조회
    long countInspectionHistory(@Param("panelId") Long panelId,
                               @Param("fromAt") LocalDateTime fromAt,
                               @Param("toAt") LocalDateTime toAt);

    // 점검 실행 1건의 항목별 결과 조회 (항목명 포함)
    List<InspectionResultItemRes> findResultItemsByInspectionId(@Param("inspectionId") Long inspectionId);

    // 점검 이력 엑셀 다운로드용 항목별 행 조회
    List<InspectionExportRowRes> findInspectionExportRows(@Param("panelId") Long panelId,
                                                          @Param("fromAt") LocalDateTime fromAt,
                                                          @Param("toAt") LocalDateTime toAt);
}
