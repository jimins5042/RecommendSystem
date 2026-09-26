package shop.RecommendSystem.accessLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.repository.mapper.AccessLogMapper;

/**
 * 보관기간이 지난 접근 로그를 지운다.
 *
 * <p>IP·세션ID·User-Agent 는 결합하면 개인 식별이 가능한 정보다. 필요 이상으로
 * 오래 들고 있지 않는 것이 이 클래스의 목적이며, 테이블 무한 증식도 함께 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogPurger {

    private final AccessLogMapper accessLogMapper;

    @Value("${accesslog.retention-days:90}")
    private int retentionDays;

    @Value("${accesslog.suspicious-retention-days:365}")
    private int suspiciousRetentionDays;

    /** 트래픽이 적은 새벽 04:30 에 수행 */
    @Scheduled(cron = "0 30 4 * * *")
    public void purge() {

        try {
            int normal = accessLogMapper.deleteNormalOlderThan(retentionDays);
            int suspicious = accessLogMapper.deleteSuspiciousOlderThan(suspiciousRetentionDays);

            if (normal + suspicious > 0) {
                log.info("접근 로그 정리 완료 - 일반 {}건({}일 경과), 의심 {}건({}일 경과)",
                        normal, retentionDays, suspicious, suspiciousRetentionDays);
            }

        } catch (Exception e) {
            // 정리 실패가 앱에 영향을 주면 안 된다. 다음 날 다시 시도된다
            log.warn("접근 로그 정리 실패: {}", e.toString());
        }
    }
}
