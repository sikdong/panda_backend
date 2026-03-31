package panda.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsRetentionScheduler {

    private final AnalyticsService analyticsService;

    @Value("${app.analytics.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.analytics.retention-cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void purgeOldVisitEvents() {
        int deleted = analyticsService.purgeOldVisitEvents(retentionDays);
        if (deleted > 0) {
            log.info("Purged {} rows from daily_visit_events (retentionDays={})", deleted, retentionDays);
        }
    }
}
