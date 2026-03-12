package panda.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import panda.image.ImageStorageService;
import panda.listing.dto.CreateListingRequest;
import panda.listing.dto.CreateListingResponse;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.MoveInType;
import panda.listing.enums.ParkingStatus;
import panda.listing.enums.PetPolicy;
import panda.listing.enums.RoomType;

@SpringBootTest
@Import(ListingSchedulerTest.TestGeocodingConfig.class)
@ActiveProfiles("test")
class ListingSchedulerTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingRecentlyRegisteredScheduler listingRecentlyRegisteredScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
        reset(imageStorageService);
        when(imageStorageService.normalizeKey(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(imageStorageService.issuePresignedGetUrl(anyString()))
                .thenReturn("https://example.com/a.jpg");
    }

    @Test
    @DisplayName("매일 배치 실행 시 생성일이 3일을 초과한 매물의 recentlyRegistered를 false로 바꾼다")
    void markOldListingsAsNotRecentlyRegisteredUpdatesOnlyOldListings() {
        CreateListingResponse oldListing = createListing("Seoul Jung-gu old");
        CreateListingResponse recentListing = createListing("Seoul Jung-gu recent");

        jdbcTemplate.update(
                "UPDATE listings SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDate.now().atStartOfDay().minusDays(4)),
                oldListing.id()
        );
        jdbcTemplate.update(
                "UPDATE listings SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDate.now().atStartOfDay().minusDays(2)),
                recentListing.id()
        );

        listingRecentlyRegisteredScheduler.markOldListingsAsNotRecentlyRegistered();

        Listing oldAfter = listingRepository.findById(oldListing.id()).orElseThrow();
        Listing recentAfter = listingRepository.findById(recentListing.id()).orElseThrow();

        assertThat(oldAfter.isRecentlyRegistered()).isFalse();
        assertThat(recentAfter.isRecentlyRegistered()).isTrue();
    }

    private CreateListingResponse createListing(String address) {
        return listingService.create(new CreateListingRequest(
                address,
                null,
                ParkingStatus.AVAILABLE,
                ElevatorStatus.NO,
                PetPolicy.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                java.util.List.of(LoanProduct.HF_YOUTH),
                LocalDate.now(),
                10000000L,
                0L,
                false,
                false,
                null,
                MoveInType.FIXED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    @TestConfiguration
    static class TestGeocodingConfig {

        @Bean
        @Primary
        GeocodingService geocodingService() {
            return address -> new Coordinate(37.5555, 126.9780);
        }

        @Bean
        @Primary
        ImageStorageService imageStorageService() {
            return mock(ImageStorageService.class);
        }
    }
}
