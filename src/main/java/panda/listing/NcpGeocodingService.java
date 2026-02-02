package panda.listing;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class NcpGeocodingService implements GeocodingService {

    private final RestClient restClient;
    private final boolean hasApiKeys;

    public NcpGeocodingService(
            RestClient.Builder restClientBuilder,
            @Value("${app.geocoding.ncp.base-url:https://maps.apigw.ntruss.com}") String baseUrl,
            @Value("${app.geocoding.ncp.api-key-id:}") String apiKeyId,
            @Value("${app.geocoding.ncp.api-key:}") String apiKey
    ) {
        this.hasApiKeys = !apiKeyId.isBlank() && !apiKey.isBlank();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.set("x-ncp-apigw-api-key-id", apiKeyId);
                    headers.set("x-ncp-apigw-api-key", apiKey);
                })
                .build();
    }

    @Override
    public Coordinate convertAddressToCoordinate(String address) {
        String query = address == null ? "" : address.trim();
        if (query.isBlank()) {
            throw new IllegalArgumentException("Address must not be blank");
        }
        if (!hasApiKeys) {
            throw new IllegalStateException("NCP API keys are missing. Set NCP_API_KEY_ID and NCP_API_KEY.");
        }

        NcpGeocodeResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/map-geocode/v2/geocode")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .body(NcpGeocodeResponse.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to call NCP geocoding API", ex);
        }

        if (response == null || response.addresses() == null || response.addresses().isEmpty()) {
            throw new IllegalStateException("No geocoding result from NCP for address: " + query);
        }

        NcpAddress first = response.addresses().getFirst();
        try {
            double longitude = Double.parseDouble(first.x());
            double latitude = Double.parseDouble(first.y());
            return new Coordinate(latitude, longitude);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid geocoding coordinates from NCP response", ex);
        }
    }

    private record NcpGeocodeResponse(List<NcpAddress> addresses) {
    }

    private record NcpAddress(String x, String y) {
    }
}
