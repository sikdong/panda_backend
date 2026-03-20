package panda.listing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import panda.listing.dto.ListingResponse;

@Slf4j
@Service
public class ListingRecentViewedService {

    private static final String RECENT_KEY_PREFIX = "recent:";
    private static final String DEBOUNCE_KEY_PREFIX = "recent:debounce:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ListingService listingService;
    private final int maxLimit;
    private final Duration recentTtl;
    private final Duration debounceTtl;

    public ListingRecentViewedService(
            StringRedisTemplate stringRedisTemplate,
            ListingService listingService,
            @Value("${app.listing.recent-viewed.max-limit:20}") int maxLimit,
            @Value("${app.listing.recent-viewed.ttl-days:1}") long ttlDays,
            @Value("${app.listing.recent-viewed.debounce-seconds:3}") long debounceSeconds
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.listingService = listingService;
        this.maxLimit = maxLimit;
        this.recentTtl = Duration.ofDays(ttlDays);
        this.debounceTtl = Duration.ofSeconds(debounceSeconds);
    }

    public void recordView(Long listingId, HttpServletRequest request) {
        listingService.ensureExists(listingId);

        HttpSession session = getOrCreateSafeSession(request);
        String sessionId = session.getId();
        String key = toKey(sessionId);
        String value = String.valueOf(listingId);

        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(toDebounceKey(sessionId, listingId), "1", debounceTtl);
            if (Boolean.FALSE.equals(acquired)) {
                return;
            }
            stringRedisTemplate.opsForList().remove(key, 0, value);
            stringRedisTemplate.opsForList().leftPush(key, value);
            stringRedisTemplate.opsForList().trim(key, 0, maxLimit - 1);
            stringRedisTemplate.expire(key, recentTtl);
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to update recent viewed listings [sessionHash={}, listingId={}]",
                    maskSessionId(sessionId),
                    listingId,
                    ex
            );
        }
    }

    public List<ListingResponse> getRecentViewed(HttpServletRequest request, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        HttpSession session = request.getSession(false);
        if (session == null) {
            return List.of();
        }

        String sessionId = session.getId();
        String key = toKey(sessionId);

        List<String> rawIds;
        try {
            rawIds = stringRedisTemplate.opsForList().range(key, 0, safeLimit - 1);
        } catch (RuntimeException ex) {
            log.warn("Failed to read recent viewed listings [sessionHash={}]", maskSessionId(sessionId), ex);
            return List.of();
        }

        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }

        List<Long> orderedIds = rawIds.stream()
                .map(this::parseLongOrNull)
                .filter(Objects::nonNull)
                .toList();
        if (orderedIds.isEmpty()) {
            pruneInvalidIds(key, rawIds, List.of());
            return List.of();
        }

        List<ListingResponse> responses = listingService.getVisibleSummariesByIdsInOrder(orderedIds);
        pruneInvalidIds(key, rawIds, responses);
        return responses;
    }

    private int normalizeLimit(Integer limit) {
        int requestedLimit = limit == null ? maxLimit : limit;
        if (requestedLimit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }
        return Math.min(requestedLimit, maxLimit);
    }

    private String toKey(String sessionId) {
        return RECENT_KEY_PREFIX + sessionId;
    }

    private String toDebounceKey(String sessionId, Long listingId) {
        return DEBOUNCE_KEY_PREFIX + sessionId + ":" + listingId;
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void pruneInvalidIds(String key, List<String> rawIds, List<ListingResponse> responses) {
        Set<Long> validIds = responses.stream()
                .map(ListingResponse::id)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        for (String rawId : rawIds) {
            Long parsedId = parseLongOrNull(rawId);
            if (parsedId == null || !validIds.contains(parsedId)) {
                try {
                    stringRedisTemplate.opsForList().remove(key, 0, rawId);
                } catch (RuntimeException ex) {
                    log.warn("Failed to prune invalid recent viewed id [value={}]", rawId, ex);
                }
            }
        }
    }

    private String maskSessionId(String sessionId) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(sessionId.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6 && i < digest.length; i++) {
                builder.append(String.format("%02x", digest[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "masked";
        }
    }

    private HttpSession getOrCreateSafeSession(HttpServletRequest request) {
        if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
            HttpSession existing = request.getSession(false);
            if (existing != null) {
                existing.invalidate();
            }
        }
        return request.getSession(true);
    }
}
