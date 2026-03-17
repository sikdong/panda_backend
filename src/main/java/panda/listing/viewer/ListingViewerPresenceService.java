package panda.listing.viewer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ListingViewerPresenceService {

    private final long ttlMs;
    private final ConcurrentMap<Long, ConcurrentMap<String, Long>> listingViewers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> sessionToListing = new ConcurrentHashMap<>();

    public ListingViewerPresenceService(
            @Value("${app.viewer-presence.ttl-ms:75000}") long ttlMs
    ) {
        this.ttlMs = ttlMs;
    }

    public synchronized int enter(Long listingId, String viewerSessionId) {
        long now = System.currentTimeMillis();
        cleanupExpiredInternal(now);

        Long previousListingId = sessionToListing.put(viewerSessionId, listingId);
        if (previousListingId != null && !previousListingId.equals(listingId)) {
            removeFromListing(previousListingId, viewerSessionId);
        }

        listingViewers
                .computeIfAbsent(listingId, id -> new ConcurrentHashMap<>())
                .put(viewerSessionId, now);

        return listingViewers.getOrDefault(listingId, new ConcurrentHashMap<>()).size();
    }

    public synchronized int getViewerCount(Long listingId, String viewerSessionId) {
        long now = System.currentTimeMillis();
        cleanupExpiredInternal(now);

        if (viewerSessionId != null && !viewerSessionId.isBlank()) {
            Long currentListingId = sessionToListing.get(viewerSessionId);
            if (currentListingId != null && currentListingId.equals(listingId)) {
                listingViewers
                        .computeIfAbsent(listingId, id -> new ConcurrentHashMap<>())
                        .put(viewerSessionId, now);
            }
        }

        return listingViewers.getOrDefault(listingId, new ConcurrentHashMap<>()).size();
    }

    public synchronized int leave(Long listingId, String viewerSessionId) {
        removeFromListing(listingId, viewerSessionId);
        sessionToListing.remove(viewerSessionId, listingId);
        return listingViewers.getOrDefault(listingId, new ConcurrentHashMap<>()).size();
    }

    @Scheduled(fixedDelayString = "${app.viewer-presence.cleanup-interval-ms:30000}")
    public synchronized void cleanupExpired() {
        cleanupExpiredInternal(System.currentTimeMillis());
    }

    private void cleanupExpiredInternal(long now) {
        long expireBefore = now - ttlMs;

        for (Map.Entry<String, Long> entry : sessionToListing.entrySet()) {
            String sessionId = entry.getKey();
            Long listingId = entry.getValue();

            ConcurrentMap<String, Long> sessions = listingViewers.get(listingId);
            if (sessions == null) {
                sessionToListing.remove(sessionId, listingId);
                continue;
            }

            Long lastSeen = sessions.get(sessionId);
            if (lastSeen == null || lastSeen < expireBefore) {
                sessions.remove(sessionId);
                sessionToListing.remove(sessionId, listingId);
                if (sessions.isEmpty()) {
                    listingViewers.remove(listingId, sessions);
                }
            }
        }
    }

    private void removeFromListing(Long listingId, String viewerSessionId) {
        ConcurrentMap<String, Long> sessions = listingViewers.get(listingId);
        if (sessions == null) {
            return;
        }
        sessions.remove(viewerSessionId);
        if (sessions.isEmpty()) {
            listingViewers.remove(listingId, sessions);
        }
    }
}
