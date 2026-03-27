package panda.image;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Query("""
            SELECT i.imagePath
            FROM Image i
            WHERE i.listing.id = :listingId
            """)
    List<String> findImagePathsByListingId(@Param("listingId") Long listingId);

    @Modifying
    @Query("""
            DELETE FROM Image i
            WHERE i.listing.id = :listingId
            """)
    void deleteByListingId(@Param("listingId") Long listingId);
}
