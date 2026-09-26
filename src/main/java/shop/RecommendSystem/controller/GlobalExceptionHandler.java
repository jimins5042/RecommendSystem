package shop.RecommendSystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import shop.RecommendSystem.accessLog.AccessLogFilter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 전역 예외 처리기.
 *
 * <p>화면(뷰) 요청은 error.html 로 넘겨 "에러가 발생했습니다!" alert 후 메인으로 보내고,
 * ajax 요청({@code @ResponseBody})은 기존 프론트 코드가 r.ok / r.json() 을 검사하므로 JSON 으로 응답한다.
 *
 * <p>예외 메시지는 내부 구조 노출을 막기 위해 화면/응답에 싣지 않고 로그에만 남긴다.
 * 대신 추적용 errorId 를 함께 내려 로그와 대조할 수 있게 한다.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        HttpStatus status = resolveStatus(e);
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String path = request.getRequestURI();

        // 접근 로그(access_log.error_id)에서 이 요청을 되짚을 수 있도록 남긴다
        request.setAttribute(AccessLogFilter.ERROR_ID_ATTRIBUTE, errorId);

        if (status.is5xxServerError()) {
            log.error("[{}] 처리되지 않은 예외 - {} {} ", errorId, request.getMethod(), path, e);
        } else if (status == HttpStatus.NOT_FOUND) {
            // 존재하지 않는 경로 요청은 대부분 스캐너/봇의 취약점 탐색이므로 WARN 으로 올리지 않는다
            log.debug("[{}] 존재하지 않는 경로 - {} {}", errorId, request.getMethod(), path);
        } else {
            log.warn("[{}] 요청 처리 실패({}) - {} {} : {}",
                    errorId, status.value(), request.getMethod(), path, e.toString());
        }

        // 정적 리소스 404 는 페이지 이동이 아닌 <img> 등 하위 요청인 경우가 많으므로 본문 없이 반환
        if (e instanceof NoResourceFoundException && !acceptsHtml(request)) {
            return ResponseEntity.status(status).build();
        }

        // 응답이 이미 커밋된 뒤(스트리밍 등)에는 뷰/바디를 덮어쓸 수 없다
        if (response.isCommitted()) {
            log.warn("[{}] 응답이 이미 커밋되어 에러 페이지를 렌더링할 수 없음", errorId);
            return ResponseEntity.status(status).build();
        }

        if (isAjaxRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status.value());
            body.put("message", "에러가 발생했습니다!");
            body.put("errorId", errorId);
            body.put("path", path);
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }

        ModelAndView mav = new ModelAndView(ERROR_VIEW);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("errorId", errorId);
        return mav;
    }

    /**
     * 예외에 선언된 상태코드를 우선 사용하고, 없으면 500 으로 본다.
     */
    private HttpStatus resolveStatus(Exception e) {

        if (e instanceof MaxUploadSizeExceededException) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }

        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus resolved = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (resolved != null) {
                return resolved;
            }
        }

        ResponseStatus annotation =
                AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
        if (annotation != null) {
            return annotation.code();
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 핸들러에 {@code @ResponseBody} 가 붙은 ajax 요청인지 판별. 핸들러가 없는 요청(정적 리소스 등)은 false.
     */
    private boolean isAjaxRequest(HttpServletRequest request) {

        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.hasMethodAnnotation(ResponseBody.class)
                    || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), ResponseBody.class);
        }

        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private boolean acceptsHtml(HttpServletRequest request) {

        String accept = request.getHeader("Accept");
        return accept == null || accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
