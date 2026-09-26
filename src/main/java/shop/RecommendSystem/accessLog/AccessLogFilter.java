package shop.RecommendSystem.accessLog;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import shop.RecommendSystem.dto.AccessLog;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

/**
 * 모든 요청을 접근 로그로 남긴다.
 *
 * <p>HandlerInterceptor 가 아니라 Filter 인 이유: 취약점 스캐너의 요청은 컨트롤러에
 * 도달하지 않고 404 로 끝나는 경우가 많다. 인터셉터로는 그런 요청이 기록되지 않는다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter extends OncePerRequestFilter {

    /** GlobalExceptionHandler 가 발급한 추적 ID 를 필터로 넘기는 통로 */
    public static final String ERROR_ID_ATTRIBUTE = "shop.RecommendSystem.errorId";

    /** 기록하지 않는 경로 — 정적 리소스와 헬스체크는 남겨봐야 용량만 먹는다 */
    private static final String[] EXCLUDED_PREFIXES = {
            "/css/", "/js/", "/images/", "/favicon.ico", "/robots.txt", "/actuator/health"
    };

    /** 취약점 스캔으로 간주하는 경로 조각 */
    private static final String[] SUSPICIOUS_PATTERNS = {
            "/actuator", "/cgi-bin", "/.env", "/.git", "/.ssh", "/.aws",
            "/wp-admin", "/wp-login", "/phpmyadmin", "/jolokia", "/druid",
            "/solr", "/console", "/shell", "/eval", "..", "%00"
    };

    private static final int MAX_URI = 500;
    private static final int MAX_QUERY = 1000;
    private static final int MAX_AGENT = 500;
    private static final int MAX_REFERER = 500;
    private static final int MAX_IP = 45;

    private final AccessLogWriter writer;
    private final boolean enabled;
    private final String uploadPrefix;

    public AccessLogFilter(AccessLogWriter writer,
                           @Value("${accesslog.enabled:true}") boolean enabled,
                           @Value("${connectPath:/upload}") String connectPath) {

        this.writer = writer;
        this.enabled = enabled;
        this.uploadPrefix = connectPath.endsWith("/") ? connectPath : connectPath + "/";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!enabled || isExcluded(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        long startedAt = System.nanoTime();

        try {
            chain.doFilter(request, response);
        } finally {
            // 기록 실패가 요청에 영향을 주면 안 된다
            try {
                writer.enqueue(build(request, response, startedAt));
            } catch (Exception e) {
                log.debug("접근 로그 생성 실패: {}", e.toString());
            }
        }
    }

    private AccessLog build(HttpServletRequest request, HttpServletResponse response, long startedAt) {

        String uri = request.getRequestURI();
        String query = request.getQueryString();

        return AccessLog.builder()
                .logTime(new Date())
                .clientIp(truncate(resolveClientIp(request), MAX_IP))
                .method(request.getMethod())
                .uri(truncate(uri, MAX_URI))
                .queryString(truncate(query, MAX_QUERY))
                .status(response.getStatus())
                .durationMs((System.nanoTime() - startedAt) / 1_000_000L)
                .userAgent(truncate(request.getHeader("User-Agent"), MAX_AGENT))
                .referer(truncate(request.getHeader("Referer"), MAX_REFERER))
                .sessionId(resolveSessionId(request))
                .errorId((String) request.getAttribute(ERROR_ID_ATTRIBUTE))
                .suspicious(isSuspicious(uri, query) ? "Y" : "N")
                .build();
    }

    /**
     * nginx 뒤에 있으므로 getRemoteAddr() 는 항상 127.0.0.1 이다.
     * X-Forwarded-For 의 첫 항목이 실제 클라이언트다.
     *
     * <p>주의: 이 헤더는 클라이언트가 위조할 수 있다. 정확한 접속 지점이 필요하면
     * nginx access log 를 함께 봐야 한다.
     */
    private String resolveClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty()) {
                return first;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 세션이 없으면 만들지 않는다. 봇 요청마다 세션을 생성하면 메모리가 남아나지 않는다.
     */
    private String resolveSessionId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        return (session != null) ? session.getId() : null;
    }

    private boolean isExcluded(String uri) {

        if (uri.startsWith(uploadPrefix)) {
            return true;
        }

        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSuspicious(String uri, String query) {

        String target = (uri + (query == null ? "" : "?" + query)).toLowerCase(Locale.ROOT);

        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (target.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 컬럼 길이를 넘기면 ORA-12899 로 배치 전체가 실패한다. 반드시 잘라서 넣는다.
     */
    private String truncate(String value, int max) {

        if (value == null) {
            return null;
        }

        return (value.length() <= max) ? value : value.substring(0, max);
    }
}
