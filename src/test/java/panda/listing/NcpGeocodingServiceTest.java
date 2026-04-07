package panda.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NcpGeocodingServiceTest {

    @Test
    @DisplayName("주소가 공백이면 좌표 변환 시 예외가 발생한다")
    void convertAddressToCoordinateThrowsWhenAddressIsBlank() {
        NcpGeocodingService service = new NcpGeocodingService(
                RestClient.builder(),
                "http://localhost:9999",
                "test-id",
                "test-key"
        );

        assertThatThrownBy(() -> service.convertAddressToCoordinate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address must not be blank");
    }

    @Test
    @DisplayName("NCP API 키가 없으면 좌표 변환 시 예외가 발생한다")
    void convertAddressToCoordinateThrowsWhenApiKeysAreMissing() {
        NcpGeocodingService service = new NcpGeocodingService(
                RestClient.builder(),
                "http://localhost:9999",
                "",
                ""
        );

        assertThatThrownBy(() -> service.convertAddressToCoordinate("Seoul Gangseo-gu Gonghang-daero 1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NCP API keys are missing");
    }

    @Test
    @DisplayName("NCP 지오코딩 응답이 정상이면 좌표 변환에 성공한다")
    void convertAddressToCoordinateReturnsCoordinateWhenResponseIsValid() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/map-geocode/v2/geocode", exchange -> {
            String body = "{\"addresses\":[{\"x\":\"126.9780\",\"y\":\"37.5665\"}]}";
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            NcpGeocodingService service = new NcpGeocodingService(
                    RestClient.builder(),
                    baseUrl,
                    "test-id",
                    "test-key"
            );

            Coordinate coordinate = service.convertAddressToCoordinate("Seoul City Hall");

            assertThat(coordinate.latitude()).isEqualTo(37.5665);
            assertThat(coordinate.longitude()).isEqualTo(126.9780);
        } finally {
            server.stop(0);
        }
    }
}
