package shop.RecommendSystem.accessLog;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 접근 로그 정리 스케줄러를 켜기 위한 설정.
 *
 * <p>{@link AccessLogPurger} 와 분리한 이유: 매퍼를 주입받는 클래스에
 * {@code @Configuration} 을 붙이면 빈 초기화 순서가 꼬일 수 있다.
 */
@Configuration
@EnableScheduling
public class AccessLogSchedulingConfig {
}
