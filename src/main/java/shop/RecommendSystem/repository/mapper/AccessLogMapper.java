package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shop.RecommendSystem.dto.AccessLog;

import java.util.List;

@Mapper
public interface AccessLogMapper {

    // 접근 로그 1건 저장 (배치 세션에서 반복 호출)
    void insert(AccessLog accessLog);

    // 보관기간이 지난 일반 요청 삭제
    int deleteNormalOlderThan(@Param("days") int days);

    // 보관기간이 지난 의심 요청 삭제
    int deleteSuspiciousOlderThan(@Param("days") int days);

    // 최근 의심 요청 조회 (운영 확인용)
    List<AccessLog> findRecentSuspicious(@Param("limit") int limit);
}
