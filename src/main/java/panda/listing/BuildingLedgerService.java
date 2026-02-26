package panda.listing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import panda.listing.dto.BuildingLedgerTitleResponse;
import panda.listing.dto.BuildingLedgerExclusivityResponse;

@Service
public class BuildingLedgerService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;
    private final String baseUrl;

    public BuildingLedgerService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.building-ledger.base-url:https://apis.data.go.kr/1613000/BldRgstService}") String baseUrl,
            @Value("${app.building-ledger.service-key:}") String serviceKey
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    /**
     * 표제부 조회 (getBrTitleInfo)
     */
    public BuildingLedgerTitleResponse getTitleInfo(
            String sigunguCd, String bjdongCd, String platGbCd, String bun, String ji
    ) {
        validateServiceKey();

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/getBrTitleInfo")
                .queryParam("serviceKey", serviceKey)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("bjdongCd", bjdongCd)
                .queryParam("platGbCd", platGbCd)
                .queryParam("bun", bun)
                .queryParam("ji", ji)
                .queryParam("_type", "json")
                .build(true)
                .toUri();
        uri= URI.create("http://apis.data.go.kr/1613000/BldRgstHubService/getBrTitleInfo?serviceKey=9497310aa0048d642dcc7ac7a825c10a9f9a2505d9a62be6ac6c1fb1a38a09a2&sigunguCd=11620&bjdongCd=10100&platGbCd=0&bun=1596&ji=0017&_type=json");
        return callApiAndMapToTitleResponse(uri);
    }

    /**
     * 전유공용면적 조회 (getBrExposPubuseAreaInfo)
     */
    public BuildingLedgerExclusivityResponse getExclusivityInfo(
            String sigunguCd, String bjdongCd, String platGbCd, String bun, String ji, String dongNm, String hoNm
    ) {
        validateServiceKey();

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/getBrExposPubuseAreaInfo")
                .queryParam("serviceKey", serviceKey)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("bjdongCd", bjdongCd)
                .queryParam("platGbCd", platGbCd)
                .queryParam("bun", bun)
                .queryParam("ji", ji)
                .queryParam("dongNm", dongNm)
                .queryParam("hoNm", hoNm)
                .queryParam("_type", "json")
                .build(true)
                .toUri();

        return callApiAndMapToExclusivityResponse(uri);
    }

    private BuildingLedgerTitleResponse callApiAndMapToTitleResponse(URI uri) {
        Map<String, Object> responseMap = requestResponseMap(uri);

        List<Map<String, Object>> rawItems = extractRawItems(responseMap);
        List<BuildingLedgerTitleResponse.TitleItem> items = rawItems.stream()
                .map(m -> new BuildingLedgerTitleResponse.TitleItem(
                        Objects.toString(m.get("mgmBldrgstPk"), ""),
                        Objects.toString(m.get("dongNm"), ""),
                        Objects.toString(m.get("mainPurpsCdNm"), ""),
                        Objects.toString(m.get("grndFlrCnt"), "0"),
                        Objects.toString(m.get("ugrndFlrCnt"), "0"),
                        Objects.toString(m.get("totArea"), "0"),
                        Objects.toString(m.get("useAprvDe"), "")
                ))
                .toList();

        return new BuildingLedgerTitleResponse(
                new BuildingLedgerTitleResponse.Data(
                        new BuildingLedgerTitleResponse.Items(items)
                )
        );
    }

    private BuildingLedgerExclusivityResponse callApiAndMapToExclusivityResponse(URI uri) {
        Map<String, Object> responseMap = requestResponseMap(uri);

        List<Map<String, Object>> rawItems = extractRawItems(responseMap);
        List<BuildingLedgerExclusivityResponse.ExclusivityItem> items = rawItems.stream()
                .map(m -> new BuildingLedgerExclusivityResponse.ExclusivityItem(
                        Objects.toString(m.get("bldNm"), ""),
                        Objects.toString(m.get("dongNm"), ""),
                        Objects.toString(m.get("hoNm"), ""),
                        Objects.toString(m.get("flrNo"), ""),
                        Objects.toString(m.get("area"), "0"),
                        Objects.toString(m.get("mainPurpsCdNm"), "")
                ))
                .toList();

        return new BuildingLedgerExclusivityResponse(
                new BuildingLedgerExclusivityResponse.Data(
                        new BuildingLedgerExclusivityResponse.Items(items)
                )
        );
    }

    private void validateServiceKey() {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("Building Ledger Service Key is missing. Set app.building-ledger.service-key.");
        }
    }

    private Map<String, Object> requestResponseMap(URI uri) {
        String responseBody = restClient.get()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Building Ledger API returned empty body. uri=" + uri);
        }

        try {
            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() { });
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to parse Building Ledger response as JSON. uri=" + uri + ", body=" + truncate(responseBody),
                    ex
            );
        }
    }

    private String truncate(String value) {
        int maxLength = 500;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRawItems(Map<String, Object> responseMap) {
        if (responseMap == null || !responseMap.containsKey("response")) {
            return Collections.emptyList();
        }
        Map<String, Object> response = (Map<String, Object>) responseMap.get("response");
        Map<String, Object> body = (Map<String, Object>) response.get("body");
        if (body == null || body.get("items") == null || "".equals(body.get("items"))) {
            return Collections.emptyList();
        }
        Map<String, Object> itemsMap = (Map<String, Object>) body.get("items");
        Object itemObj = itemsMap.get("item");

        if (itemObj instanceof List) {
            return (List<Map<String, Object>>) itemObj;
        } else if (itemObj instanceof Map) {
            return List.of((Map<String, Object>) itemObj);
        }
        return Collections.emptyList();
    }
}
