package panda.listing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UriComponentsBuilder;
import panda.listing.dto.BuildingLedgerExclusivityResponse;
import panda.listing.dto.BuildingLedgerTitleResponse;

@Service
public class BuildingLedgerService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_NUM_OF_ROWS = 100;

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
     * Title ledger lookup (getBrTitleInfo)
     */
    public BuildingLedgerTitleResponse getTitleInfo(
            String sigunguCd, String bjdongCd, String platGbCd, String bun, String ji
    ) {
        validateServiceKey();

        List<Map<String, Object>> rawItems = fetchPagedItems(pageNo -> UriComponentsBuilder.fromUriString(baseUrl)
                .path("/getBrTitleInfo")
                .queryParam("serviceKey", serviceKey)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("bjdongCd", bjdongCd)
                .queryParam("platGbCd", platGbCd)
                .queryParam("bun", bun)
                .queryParam("ji", ji)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
                .queryParam("_type", "json")
                .build(true)
                .toUri());

        BuildingLedgerTitleResponse buildingLedgerTitleResponse = mapToTitleResponse(rawItems);
        return buildingLedgerTitleResponse;
    }

    /**
     * Exclusivity/public-use area lookup (getBrExposPubuseAreaInfo)
     */
    public BuildingLedgerExclusivityResponse getExclusivityInfo(
            String sigunguCd, String bjdongCd, String platGbCd, String bun, String ji, String dongNm, String hoNm
    ) {
        validateServiceKey();

        List<Map<String, Object>> rawItems = fetchPagedItems(pageNo -> UriComponentsBuilder.fromUriString(baseUrl)
                .path("/getBrExposPubuseAreaInfo")
                .queryParam("serviceKey", serviceKey)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("bjdongCd", bjdongCd)
                .queryParam("platGbCd", platGbCd)
                .queryParam("bun", bun)
                .queryParam("ji", ji)
                .queryParam("dongNm", encodeQueryParam(dongNm))
                .queryParam("hoNm", encodeQueryParam(hoNm))
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
                .queryParam("_type", "json")
                .build(true)
                .toUri());
        System.out.println("rawItems  === "+ rawItems);
        BuildingLedgerExclusivityResponse buildingLedgerExclusivityResponse = mapToExclusivityResponse(rawItems);
        System.out.println(buildingLedgerExclusivityResponse);
        return buildingLedgerExclusivityResponse;
    }

    private BuildingLedgerTitleResponse mapToTitleResponse(List<Map<String, Object>> rawItems) {
        List<BuildingLedgerTitleResponse.TitleItem> items = rawItems.stream()
                .map(m -> {
                    int parkingCount = resolveParkingCount(m);
                    return new BuildingLedgerTitleResponse.TitleItem(
                            getString(m, "mgmBldrgstPk", ""),
                            getString(m, "dongNm", ""),
                            getString(m, "grndFlrCnt", "0"),
                            getFirstString(m, "", "useAprDay", "useAprvDe"),
                            Integer.toString(parkingCount),
                            parkingCount > 0
                    );
                })
                .toList();

        return new BuildingLedgerTitleResponse(
                new BuildingLedgerTitleResponse.Data(
                        new BuildingLedgerTitleResponse.Items(items)
                )
        );
    }

    private BuildingLedgerExclusivityResponse mapToExclusivityResponse(List<Map<String, Object>> rawItems) {
        List<BuildingLedgerExclusivityResponse.ExclusivityItem> items = rawItems.stream()
                .filter(this::isExclusiveArea)
                .map(m -> new BuildingLedgerExclusivityResponse.ExclusivityItem(
                        getString(m, "flrNo", ""),
                        getString(m, "area", "0")
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

    private List<Map<String, Object>> fetchPagedItems(PageUriBuilder pageUriBuilder) {
        List<Map<String, Object>> allItems = new ArrayList<>();
        int pageNo = DEFAULT_PAGE_NO;

        while (true) {
            URI uri = pageUriBuilder.build(pageNo);
            Map<String, Object> responseMap = requestResponseMap(uri);
            List<Map<String, Object>> pageItems = extractRawItems(responseMap);
            allItems.addAll(pageItems);

            int totalCount = extractTotalCount(responseMap);
            if (totalCount <= 0 || allItems.size() >= totalCount) {
                break;
            }
            if (pageItems.isEmpty()) {
                break;
            }
            pageNo++;
        }

        return allItems;
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

    private boolean isExclusiveArea(Map<String, Object> item) {
        String exposPubuseGbCdNm = getString(item, "exposPubuseGbCdNm", "");
        return "\uC804\uC720".equals(exposPubuseGbCdNm);
    }

    private int resolveParkingCount(Map<String, Object> item) {
        int totalParking = getInt(item, "totPkngCnt");
        if (totalParking > 0) {
            return totalParking;
        }
        return getInt(item, "indrAutoUtcnt")
                + getInt(item, "oudrAutoUtcnt")
                + getInt(item, "indrMechUtcnt")
                + getInt(item, "oudrMechUtcnt");
    }

    private int getInt(Map<String, Object> item, String key) {
        String value = getString(item, key, "0");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            try {
                return (int) Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }

    private String getString(Map<String, Object> item, String key, String defaultValue) {
        Object value = item.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private String getFirstString(Map<String, Object> item, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = getString(item, key, "");
            if (!value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

    private String encodeQueryParam(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private int extractTotalCount(Map<String, Object> responseMap) {
        if (responseMap == null || !responseMap.containsKey("response")) {
            return 0;
        }
        Map<String, Object> response = (Map<String, Object>) responseMap.get("response");
        if (response == null || !(response.get("body") instanceof Map<?, ?> bodyObj)) {
            return 0;
        }
        Map<String, Object> body = (Map<String, Object>) bodyObj;
        Object totalCountObj = body.get("totalCount");
        if (totalCountObj == null) {
            return 0;
        }
        try {
            return Integer.parseInt(totalCountObj.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
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

    @FunctionalInterface
    private interface PageUriBuilder {
        URI build(int pageNo);
    }
}
