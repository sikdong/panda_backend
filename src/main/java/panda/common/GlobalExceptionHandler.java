package panda.common;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern LISTING_VIEW_PATH_PATTERN = Pattern.compile("^/api/v1/listings/\\d+/view$");

    private final SlackNotifier slackNotifier;

    public GlobalExceptionHandler(SlackNotifier slackNotifier) {
        this.slackNotifier = slackNotifier;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("errorType", "INPUT_ERROR");
        response.put("message", "입력값이 올바르지 않습니다.");
        response.put("fields", fields);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String requestUri = request.getRequestURI();
        if (LISTING_VIEW_PATH_PATTERN.matcher(requestUri).matches()) {
            log.warn(
                    "Method not supported on listing view endpoint: method={}, uri={}, supported={}, userAgent={}, xForwardedFor={}, referer={}, remoteAddr={}",
                    request.getMethod(),
                    requestUri,
                    ex.getSupportedHttpMethods(),
                    request.getHeader("User-Agent"),
                    request.getHeader("X-Forwarded-For"),
                    request.getHeader("Referer"),
                    request.getRemoteAddr()
            );
        }

        log.info(
                "Method not supported: method={}, uri={}, supported={}",
                request.getMethod(),
                requestUri,
                ex.getSupportedHttpMethods()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("errorType", "METHOD_NOT_ALLOWED");
        response.put("message", "허용되지 않은 HTTP 메서드입니다.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleServer(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception", ex);
        if (!isClientRequestError(ex)) {
            slackNotifier.notifyUnhandledException(ex, request);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("errorType", "SERVER_ERROR");
        response.put("message", "서버 처리 중 오류가 발생했습니다.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private boolean isClientRequestError(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode().is4xxClientError();
        }
        return false;
    }
}
