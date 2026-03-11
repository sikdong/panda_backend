package panda.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "daily_actor_activity",
        indexes = @Index(name = "idx_daily_actor_date", columnList = "event_date_kst"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_actor",
                columnNames = {"event_date_kst", "actor_key"}
        )
)
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyActorActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date_kst", nullable = false)
    private LocalDate eventDateKst;

    @Column(name = "actor_key", nullable = false, length = 128)
    private String actorKey;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @PrePersist
    public void onCreate() {
        if (firstSeenAt == null) {
            firstSeenAt = LocalDateTime.now();
        }
    }
}
