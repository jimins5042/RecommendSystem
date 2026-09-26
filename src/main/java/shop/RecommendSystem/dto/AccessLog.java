package shop.RecommendSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 요청 1건에 대한 접근 기록.
 *
 * <p>요청 본문은 남기지 않는다. 이미지 업로드가 섞여 있어 용량이 감당되지 않고,
 * 남겨서 얻을 이득도 없다.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class AccessLog {

    private Long logId;
    private Date logTime;
    private String clientIp;
    private String method;
    private String uri;
    private String queryString;
    private Integer status;
    private Long durationMs;
    private String userAgent;
    private String referer;
    private String sessionId;

    /** GlobalExceptionHandler 가 발급한 추적 ID. 에러가 없으면 null */
    private String errorId;

    /** 취약점 스캔으로 보이는 경로면 'Y' */
    private String suspicious;

    public AccessLog() {
    }
}
