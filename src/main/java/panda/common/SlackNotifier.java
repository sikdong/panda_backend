package panda.common;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class SlackNotifier {

    private static final int MAX_STACK_TRACE_LENGTH = 1500;
    private static final DateTimeFormatter ALERT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient restClient;
    private final boolean enabled;
    private final String webhookUrl;

    public SlackNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${app.alert.slack.enabled:false}") boolean enabled,
            @Value("${app.alert.slack.webhook-url:}") String webhookUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
    }

    public void notifyUnhandledException(Exception ex, HttpServletRequest request) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        Thread.startVirtualThread(() -> sendUnhandledException(ex, request));
    }

    private void sendUnhandledException(Exception ex, HttpServletRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", buildMessage(ex, request));

            ResponseEntity<String> response = restClient.post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);
        } catch (Exception notifyException) {
            log.warn("Failed to send Slack error alert", notifyException);
        }
    }

    private String buildMessage(Exception ex, HttpServletRequest request) {
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String stackTrace = getStackTrace(ex);

        return """
                [Panda Server Error]
                time: %s
                request: %s %s
                exception: *%s*
                message: *%s*
                stackTrace:
                %s
                """.formatted(
                LocalDateTime.now().format(ALERT_TIME_FORMATTER),
                method,
                uri,
                ex.getClass().getName(),
                ex.getMessage(),
                stackTrace
        );
    }

    private String getStackTrace(Exception ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        String raw = stringWriter.toString();

        if (raw.length() <= MAX_STACK_TRACE_LENGTH) {
            return raw;
        }
        return raw.substring(0, MAX_STACK_TRACE_LENGTH) + "\n...(truncated)";
    }
}
