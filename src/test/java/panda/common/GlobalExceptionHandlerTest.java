package panda.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("서버 예외 발생 시 SlackNotifier를 호출하고 500 응답을 반환한다")
    void handleServerCallsSlackNotifierAndReturnsInternalServerError() {
        SlackNotifier slackNotifier = mock(SlackNotifier.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(slackNotifier);
        RuntimeException exception = new RuntimeException("slack test");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/listings/1");

        ResponseEntity<Map<String, Object>> response = handler.handleServer(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("errorType", "SERVER_ERROR");
        verify(slackNotifier).notifyUnhandledException(exception, request);
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405를 반환하고 Slack 알림을 보내지 않는다")
    void handleMethodNotSupportedReturnsMethodNotAllowedAndDoesNotCallSlack() {
        SlackNotifier slackNotifier = mock(SlackNotifier.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(slackNotifier);
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("GET");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/listings/123/view");

        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotSupported(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("errorType", "METHOD_NOT_ALLOWED");
        verify(slackNotifier, never()).notifyUnhandledException(exception, request);
    }
}
