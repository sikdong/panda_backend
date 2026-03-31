package panda.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AnalyticsRepository extends JpaRepository<DailyVisitEvent, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT IGNORE INTO daily_actor_activity (event_date_kst, actor_key, first_seen_at)
            VALUES (:eventDateKst, :actorKey, :now)
            """, nativeQuery = true)
    int insertDailyActorIgnore(
            @Param("eventDateKst") LocalDate eventDateKst,
            @Param("actorKey") String actorKey,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO daily_visit_events (event_date_kst, actor_key, path, occurred_at)
            VALUES (:eventDateKst, :actorKey, :path, :now)
            """, nativeQuery = true)
    int insertVisit(
            @Param("eventDateKst") LocalDate eventDateKst,
            @Param("actorKey") String actorKey,
            @Param("path") String path,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM daily_visit_events
            WHERE occurred_at < :cutoff
            """, nativeQuery = true)
    int deleteVisitsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    interface DailyMetricRow {

        String getDate();

        Integer getDau();

        Integer getVisits();
    }

    @Query(value = """
            WITH RECURSIVE days AS (
              SELECT :startDate AS d
              UNION ALL
              SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < :endDate
            ),
            dau AS (
              SELECT event_date_kst AS d, COUNT(*) AS dau
              FROM daily_actor_activity
              WHERE event_date_kst BETWEEN :startDate AND :endDate
              GROUP BY event_date_kst
            ),
            visits AS (
              SELECT event_date_kst AS d, COUNT(*) AS visits
              FROM daily_visit_events
              WHERE event_date_kst BETWEEN :startDate AND :endDate
              GROUP BY event_date_kst
            )
            SELECT DATE_FORMAT(days.d, '%Y-%m-%d') AS date,
                   COALESCE(dau.dau, 0) AS dau,
                   COALESCE(visits.visits, 0) AS visits
            FROM days
            LEFT JOIN dau ON dau.d = days.d
            LEFT JOIN visits ON visits.d = days.d
            ORDER BY days.d
            """, nativeQuery = true)
    List<DailyMetricRow> findDailyMetrics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
