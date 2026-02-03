package panda.listing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import panda.listing.dto.CreateListingRequest;
import panda.listing.dto.CreateListingResponse;
import panda.listing.dto.ListingSummaryResponse;
import panda.listing.enums.AvailabilityStatus;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

@SpringBootTest
@Import(ListingServiceTest.TestGeocodingConfig.class)
@ActiveProfiles("test")
class ListingServiceTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
    }

    @Test
    void createSavesListingWithBuilderAndReturnsId() {
        CreateListingRequest request = new CreateListingRequest(
                "  Seoul Gangnam Teheran-ro 1  ",
                "new",
                AvailabilityStatus.AVAILABLE,
                ElevatorStatus.YES,
                AvailabilityStatus.CHECK_REQUIRED,
                ContractType.MONTHLY_RENT,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.KAKAO_BANK, LoanProduct.HF_YOUTH),
                "20260214",
                10000000L,
                550000L
        );

        CreateListingResponse response = listingService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.createdAt()).isNotNull();

        Listing saved = listingRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getAddress()).isEqualTo("Seoul Gangnam Teheran-ro 1");
        assertThat(saved.getMoveInDate()).isEqualTo(LocalDate.of(2026, 2, 14));
        assertThat(saved.getLatitude()).isEqualTo(37.5555);
        assertThat(saved.getLongitude()).isEqualTo(126.9780);
        assertThat(saved.getDeposit()).isEqualTo(10000000L);
        assertThat(saved.getMonthlyRent()).isEqualTo(550000L);
        assertThat(saved.getLoanProducts()).containsExactly(LoanProduct.KAKAO_BANK, LoanProduct.HF_YOUTH);
    }

    @Test
    void getSummariesReturnsAllSavedListings() {
        listingService.create(new CreateListingRequest(
                "Seoul Mapo Worldcupbuk-ro 10",
                null,
                AvailabilityStatus.AVAILABLE,
                ElevatorStatus.NO,
                AvailabilityStatus.UNAVAILABLE,
                ContractType.JEONSE,
                RoomType.TWO_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                "20260301",
                200000000L,
                0L
        ));
        listingService.create(new CreateListingRequest(
                "Seoul Seongdong Wangsimni-ro 20",
                "remodel",
                AvailabilityStatus.CHECK_REQUIRED,
                ElevatorStatus.YES,
                AvailabilityStatus.AVAILABLE,
                ContractType.SEMI_JEONSE,
                RoomType.ONE_POINT_FIVE_ROOM,
                List.of(LoanProduct.SH, LoanProduct.KAKAO_BANK),
                "20260401",
                80000000L,
                350000L
        ));

        List<ListingSummaryResponse> summaries = listingService.getSummaries().stream()
                .sorted(Comparator.comparing(ListingSummaryResponse::address))
                .toList();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).address()).isEqualTo("Seoul Mapo Worldcupbuk-ro 10");
        assertThat(summaries.get(0).contractType()).isEqualTo(ContractType.JEONSE);
        assertThat(summaries.get(1).address()).isEqualTo("Seoul Seongdong Wangsimni-ro 20");
        assertThat(summaries.get(1).contractType()).isEqualTo(ContractType.SEMI_JEONSE);
    }

    @TestConfiguration
    static class TestGeocodingConfig {
        @Bean
        @Primary
        GeocodingService geocodingService() {
            return ignored -> new Coordinate(37.5555, 126.9780);
        }
    }
}
