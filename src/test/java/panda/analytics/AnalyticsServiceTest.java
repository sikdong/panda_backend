package panda.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @BeforeEach
    void setUp() {
        analyticsRepository.deleteAll();
    }

    @Test
    @DisplayName("trackVisit stores event date and occurredAt in KST")
    void trackVisitStoresDateAndTimeInKst() {
        LocalDateTime before = LocalDateTime.now(KST);
        analyticsService.trackVisit("anon_123", "/api/v1/listings");
        LocalDateTime after = LocalDateTime.now(KST);

        assertThat(analyticsRepository.count()).isEqualTo(1);

        DailyVisitEvent visitEvent = analyticsRepository.findAll().getFirst();
        assertThat(visitEvent.getActorKey()).isEqualTo("anon_123");
        assertThat(visitEvent.getPath()).isEqualTo("/api/v1/listings");
        assertThat(visitEvent.getEventDateKst()).isEqualTo(visitEvent.getOccurredAt().toLocalDate());
        assertThat(visitEvent.getOccurredAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    @DisplayName("trackVisit skips persistence when actorKey is blank")
    void trackVisitSkipsWhenActorKeyBlank() {
        analyticsService.trackVisit("   ", "/api/v1/listings");

        assertThat(analyticsRepository.count()).isZero();
    }

    @Test
    @DisplayName("purgeOldVisitEvents deletes only rows older than retention days")
    void purgeOldVisitEventsDeletesOldRowsFromDatabase() {
        int retentionDays = 30;
        LocalDateTime nowKst = LocalDateTime.now(KST);

        LocalDateTime oldOccurredAt = nowKst.minusDays(retentionDays).minusMinutes(1);
        LocalDateTime freshOccurredAt = nowKst.minusDays(retentionDays).plusMinutes(1);

        analyticsRepository.insertVisit(oldOccurredAt.toLocalDate(), "old_actor", "/old", oldOccurredAt);
        analyticsRepository.insertVisit(freshOccurredAt.toLocalDate(), "fresh_actor", "/fresh", freshOccurredAt);

        assertThat(analyticsRepository.count()).isEqualTo(2);

        int deleted = analyticsService.purgeOldVisitEvents(retentionDays);

        assertThat(deleted).isEqualTo(1);
        assertThat(analyticsRepository.count()).isEqualTo(1);

        DailyVisitEvent remaining = analyticsRepository.findAll().getFirst();
        assertThat(remaining.getActorKey()).isEqualTo("fresh_actor");
    }
}
