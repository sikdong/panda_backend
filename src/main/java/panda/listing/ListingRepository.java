package panda.listing;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findAllByOrderByUpdatedAtDesc();
    List<Listing> findBySoldFalseOrderByUpdatedAtDesc();
    List<Listing> findByAddressContainingIgnoreCaseOrderByUpdatedAtDesc(String address);
    List<Listing> findByIdInAndSoldFalse(List<Long> ids);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Listing l
            SET l.viewCount = COALESCE(l.viewCount, 0) + 1
            WHERE l.id = :id
            """)
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Listing l
            SET l.recentlyRegistered = false
            WHERE l.recentlyRegistered = true
              AND l.createdAt < :threshold
            """)
    int markOldListingsAsNotRecentlyRegistered(@Param("threshold") LocalDateTime threshold);
}
