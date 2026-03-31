package panda.analytics;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import panda.analytics.dto.AdminDauDailyMetricDto;

@Slf4j
@Service
public class AnalyticsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AnalyticsRepository analyticsRepository;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long cookieMaxAgeDays;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            @Value("${app.analytics.cookie.name:anon_id}") String cookieName,
            @Value("${app.analytics.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.analytics.cookie.same-site:None}") String cookieSameSite,
            @Value("${app.analytics.cookie.max-age-days:180}") long cookieMaxAgeDays
    ) {
        this.analyticsRepository = analyticsRepository;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieMaxAgeDays = cookieMaxAgeDays;
    }

    public String getOrCreateActorKey(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        String actorKey = "anon_" + UUID.randomUUID();
        ResponseCookie cookie = ResponseCookie.from(cookieName, actorKey)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(cookieMaxAgeDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return actorKey;
    }

    @Transactional
    public void trackVisit(String actorKey, String path) {
        if (actorKey == null || actorKey.isBlank()) {
            return;
        }

        LocalDateTime nowKst = LocalDateTime.now(KST);
        LocalDate eventDateKst = nowKst.toLocalDate();

        try {
            analyticsRepository.insertDailyActorIgnore(eventDateKst, actorKey, nowKst);
            analyticsRepository.insertVisit(eventDateKst, actorKey, path, nowKst);
        } catch (Exception e) {
            log.warn("Failed to track visit metric. actorKey={}, path={}", actorKey, path, e);
        }
    }

    @Transactional(readOnly = true)
    public List<AdminDauDailyMetricDto> getDailyMetrics(LocalDate startDate, LocalDate endDate) {
        return analyticsRepository.findDailyMetrics(startDate, endDate).stream()
                .map(row -> new AdminDauDailyMetricDto(row.getDate(), row.getDau(), row.getVisits()))
                .toList();
    }

    @Transactional
    public int purgeOldVisitEvents(int retentionDays) {
        LocalDateTime cutoffKst = LocalDateTime.now(KST).minusDays(retentionDays);
        return analyticsRepository.deleteVisitsOlderThan(cutoffKst);
    }
}
