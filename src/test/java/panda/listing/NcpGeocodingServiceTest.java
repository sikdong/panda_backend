package panda.listing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NcpGeocodingServiceTest {

    @Test
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
    void convertAddressToCoordinateThrowsWhenApiKeysAreMissing() {
        NcpGeocodingService service = new NcpGeocodingService(
                RestClient.builder(),
                "http://localhost:9999",
                "",
                ""
        );

        assertThatThrownBy(() -> service.convertAddressToCoordinate("서울시 강서구 공항대로 1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NCP API keys are missing");
    }
}
