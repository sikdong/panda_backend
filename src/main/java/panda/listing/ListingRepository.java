package panda.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findAllByOrderByUpdatedAtDesc();
    List<Listing> findBySoldFalseOrderByUpdatedAtDesc();
    List<Listing> findByAddressContainingIgnoreCaseOrderByUpdatedAtDesc(String address);
}
