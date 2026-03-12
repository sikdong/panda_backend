package panda.listing;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingRecentlyRegisteredScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ListingRepository listingRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void markOldListingsAsNotRecentlyRegistered() {
        LocalDateTime threshold = LocalDateTime.now(KST).minusDays(3);
        int updated = listingRepository.markOldListingsAsNotRecentlyRegistered(threshold);
        if (updated > 0) {
            log.info("Updated recentlyRegistered=false for {} listings (threshold={})", updated, threshold);
        }
    }
}
