package panda.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import panda.image.ImageStorageService;
import panda.listing.dto.*;
import panda.listing.enums.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(ListingServiceTest.TestGeocodingConfig.class)
@ActiveProfiles("test")
class ListingServiceTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
        reset(imageStorageService);
        when(imageStorageService.store(anyList())).thenReturn(List.of());
        when(imageStorageService.issuePresignedGetUrl(anyString())).thenReturn("https://example.com/a.jpg");
    }

    @Test
    @DisplayName("매물 생성 시 Builder 기반으로 저장되고 ID와 생성 시각이 반환된다")
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
                550000L,
                false,
                false,
                MoveInType.FIXED
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
        assertThat(saved.isSold()).isFalse();
        assertThat(saved.getLoanProducts()).containsExactly(LoanProduct.KAKAO_BANK, LoanProduct.HF_YOUTH);
    }

    @Test
    @DisplayName("매물 요약 목록 조회 시 저장된 모든 매물이 반환된다")
    void getSummariesReturnsAllSavedListings() {
        createListing("Seoul Mapo Worldcupbuk-ro 10", false);
        createListing("Seoul Seongdong Wangsimni-ro 20", true);

        List<ListingResponse> summaries = listingService.getSummaries().stream()
                .sorted(Comparator.comparing(ListingResponse::address))
                .toList();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).address()).isEqualTo("Seoul Mapo Worldcupbuk-ro 10");
        assertThat(summaries.get(0).sold()).isFalse();
        assertThat(summaries.get(1).address()).isEqualTo("Seoul Seongdong Wangsimni-ro 20");
        assertThat(summaries.get(1).sold()).isTrue();
    }

    @Test
    @DisplayName("매물 요약 목록은 updatedAt 최신순으로 정렬되어 반환된다")
    void getSummariesReturnsListingsInUpdatedAtDescOrder() {
        CreateListingResponse first = createListing("Seoul Songpa Olympic-ro 1", false);
        CreateListingResponse second = createListing("Seoul Gangseo Haneul-gil 2", false);

        listingService.patchSold(first.id(), new UpdateListingSoldRequest(true));

        List<ListingResponse> summaries = listingService.getSummaries();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).id()).isEqualTo(first.id());
        assertThat(summaries.get(1).id()).isEqualTo(second.id());
    }

    @Test
    @DisplayName("전체 매물 중 sold가 false인 매물만 최신순으로 반환한다")
    void getUnsoldSummariesReturnsOnlyUnsoldListingsInUpdatedAtDescOrder() {
        CreateListingResponse first = createListing("Seoul Seodaemun Tongil-ro 1", false);
        CreateListingResponse second = createListing("Seoul Gangdong Cheonho-daero 2", false);

        listingService.patchSold(first.id(), new UpdateListingSoldRequest(true));

        List<ListingResponse> unsold = listingService.getUnsoldListings();

        assertThat(unsold).hasSize(1);
        assertThat(unsold.getFirst().id()).isEqualTo(second.id());
        assertThat(unsold.getFirst().sold()).isFalse();
    }

    @Test
    @DisplayName("주소 검색은 부분 일치 항목만 updatedAt 최신순으로 반환한다")
    void searchByAddressReturnsMatchedListingsInUpdatedAtDescOrder() {
        CreateListingResponse first = createListing("Seoul Songpa Olympic-ro 1", false);
        CreateListingResponse second = createListing("Busan Songpa Branch 2", false);
        createListing("Incheon Bupyeong Market 3", false);

        listingService.patchSold(first.id(), new UpdateListingSoldRequest(true));

        List<ListingResponse> summaries = listingService.searchByAddress("songpa");

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).id()).isEqualTo(first.id());
        assertThat(summaries.get(1).id()).isEqualTo(second.id());
    }

    @Test
    @DisplayName("주소 검색에서 공백 키워드를 입력하면 400 예외가 발생한다")
    void searchByAddressRejectsBlankKeyword() {
        assertThatThrownBy(() -> listingService.searchByAddress("   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("매물 수정에서 sold만 변경하면 주소와 좌표는 유지된다")
    void patchUpdatesSoldWithoutChangingAddressCoordinate() {
        CreateListingResponse created = createListing("Seoul Jung Toegye-ro 1", false);

        listingService.patch(created.id(), new UpdateListingRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                MoveInType.FIXED
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.isSold()).isTrue();
        assertThat(patched.getAddress()).isEqualTo("Seoul Jung Toegye-ro 1");
        assertThat(patched.getLatitude()).isEqualTo(37.5555);
        assertThat(patched.getLongitude()).isEqualTo(126.9780);
    }

    @Test
    @DisplayName("주소가 변경되면 좌표가 재계산되어 갱신된다")
    void patchUpdatesCoordinateWhenAddressChanges() {
        CreateListingResponse created = createListing("Seoul Jung Toegye-ro 1", false);

        listingService.patch(created.id(), new UpdateListingRequest(
                "Seoul New Address 123",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                MoveInType.FIXED
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.getAddress()).isEqualTo("Seoul New Address 123");
        assertThat(patched.getLatitude()).isEqualTo(37.1234);
        assertThat(patched.getLongitude()).isEqualTo(127.5678);
    }

    @Test
    @DisplayName("주소 변경 없이 다른 필드만 수정하면 좌표는 유지된다")
    void patchKeepsCoordinateWhenAddressUnchanged() {
        CreateListingResponse created = createListing("Seoul Jung Toegye-ro 1", false);

        listingService.patch(created.id(), new UpdateListingRequest(
                "Seoul Jung Toegye-ro 1",
                "updated note",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                MoveInType.FIXED
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.getLatitude()).isEqualTo(37.5555);
        assertThat(patched.getLongitude()).isEqualTo(126.9780);
    }

    @Test
    @DisplayName("imagePaths만 변경해도 DB에 이미지 목록이 반영된다")
    @Transactional
    void patchUpdatesImagePathsWithNoNewFiles() {
        CreateListingResponse created = createListing("Seoul Jung Toegye-ro 1", false);
        Listing listing = listingRepository.findById(created.id()).orElseThrow();
        listing.addImagePath("listings/old-1.jpg");
        listing.addImagePath("listings/old-2.jpg");

        listingService.patch(created.id(), new UpdateListingRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                List.of("listings/old-2.jpg"),
                MoveInType.FIXED
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.getImages()).hasSize(1);
        assertThat(patched.getImages().getFirst().getImagePath()).isEqualTo("listings/old-2.jpg");
    }

    @Test
    @DisplayName("imagePaths에 없는 기존 이미지는 S3와 DB에서 삭제된다")
    @Transactional
    void patchRemovesImagesNotInRequest() {
        CreateListingResponse created = createListing("Seoul Jung Toegye-ro 1", false);
        Listing listing = listingRepository.findById(created.id()).orElseThrow();
        listing.addImagePath("listings/keep.jpg");
        listing.addImagePath("listings/remove.jpg");

        listingService.patch(created.id(), new UpdateListingRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                List.of("listings/keep.jpg"),
                MoveInType.FIXED
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.getImages()).hasSize(1);
        assertThat(patched.getImages().getFirst().getImagePath()).isEqualTo("listings/keep.jpg");
        verify(imageStorageService).delete(List.of("listings/remove.jpg"));
    }

    @Test
    @DisplayName("imagePaths에 존재하지 않는 경로가 포함되면 400 예외가 발생한다")
    @Transactional
    void patchRejectsUnknownImagePath() {
        CreateListingResponse created = createListing("Seoul Jung Toegye-ro 1", false);
        Listing listing = listingRepository.findById(created.id()).orElseThrow();
        listing.addImagePath("listings/existing.jpg");

        assertThatThrownBy(() -> listingService.patch(created.id(), new UpdateListingRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                List.of("listings/unknown.jpg"),
                MoveInType.FIXED
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(imageStorageService, never()).delete(anyList());
    }

    @Test
    @DisplayName("매물 삭제 후에는 해당 매물을 조회할 수 없다")
    void deleteRemovesListing() {
        CreateListingResponse created = createListing("Seoul Yongsan Hangang-daero 10", false);

        listingService.delete(created.id());

        assertThat(listingRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("sold 전용 수정은 거래 완료 상태만 변경한다")
    void patchSoldUpdatesSoldOnly() {
        CreateListingResponse created = createListing("Seoul Jongno Jong-ro 1", false);

        listingService.patchSold(created.id(), new UpdateListingSoldRequest(true));

        Listing updated = listingRepository.findById(created.id()).orElseThrow();
        assertThat(updated.isSold()).isTrue();
        assertThat(updated.getAddress()).isEqualTo("Seoul Jongno Jong-ro 1");
        assertThat(updated.getDeposit()).isEqualTo(10000000L);
    }

    @Test
    @DisplayName("create returns 400 when moveInType is FIXED and moveInDate is missing")
    void createRejectsFixedWithoutMoveInDate() {
        CreateListingRequest request = new CreateListingRequest(
                "Seoul Gangnam Teheran-ro 1",
                null,
                AvailabilityStatus.AVAILABLE,
                ElevatorStatus.YES,
                AvailabilityStatus.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                null,
                10000000L,
                500000L,
                false,
                false,
                MoveInType.FIXED
        );

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("create returns 400 when moveInType is IMMEDIATE and moveInDate is provided")
    void createRejectsImmediateWithMoveInDate() {
        CreateListingRequest request = new CreateListingRequest(
                "Seoul Gangnam Teheran-ro 1",
                null,
                AvailabilityStatus.AVAILABLE,
                ElevatorStatus.YES,
                AvailabilityStatus.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                "20260101",
                10000000L,
                500000L,
                false,
                false,
                MoveInType.IMMEDIATE
        );

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private CreateListingResponse createListing(String address, boolean sold) {
        return listingService.create(defaultRequest(address, sold));
    }

    private CreateListingRequest defaultRequest(String address, boolean sold) {
        return new CreateListingRequest(
                address,
                null,
                AvailabilityStatus.AVAILABLE,
                ElevatorStatus.NO,
                AvailabilityStatus.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                "20260101",
                10000000L,
                0L,
                sold,
                false,
                MoveInType.FIXED
        );
    }

    @TestConfiguration
    static class TestGeocodingConfig {
        @Bean
        @Primary
        GeocodingService geocodingService() {
            return address -> {
                if (address != null && address.contains("New Address")) {
                    return new Coordinate(37.1234, 127.5678);
                }
                return new Coordinate(37.5555, 126.9780);
            };
        }

        @Bean
        @Primary
        ImageStorageService imageStorageService() {
            return mock(ImageStorageService.class);
        }
    }
}
