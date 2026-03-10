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

import java.math.BigDecimal;
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
        when(imageStorageService.normalizeKey(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("매물 생성 시 Builder 기반으로 저장되고 ID와 생성 시각이 반환된다")
    void createSavesListingWithBuilderAndReturnsId() {
        CreateListingRequest request = new CreateListingRequest(
                "  Seoul Gangnam Teheran-ro 1  ",
                "new",
                ParkingStatus.AVAILABLE,
                ElevatorStatus.YES,
                PetPolicy.CHECK_REQUIRED,
                ContractType.MONTHLY_RENT,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.KAKAO_BANK, LoanProduct.HF_YOUTH),
                LocalDate.of(2026, 2, 14),
                10000000L,
                550000L,
                false,
                false,
                null,
                MoveInType.FIXED,
                new BigDecimal("18.75"),
                LocalDate.of(2020, 5, 1),
                15,
                7,
                12,
                85000L,
                LoanStatus.BELOW_30,
                IllegalBuildingStatus.NO,
                "room condition is excellent"
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
        assertThat(saved.getExclusivityArea()).isEqualByComparingTo(new BigDecimal("18.75"));
        assertThat(saved.getUseAprDay()).isEqualTo(LocalDate.of(2020, 5, 1));
        assertThat(saved.getTotalFloors()).isEqualTo(15);
        assertThat(saved.getCurrentFloor()).isEqualTo(7);
        assertThat(saved.getParkingCount()).isEqualTo(12);
        assertThat(saved.getMaintenanceFee()).isEqualTo(85000L);
        assertThat(saved.getLoanStatus()).isEqualTo(LoanStatus.BELOW_30);
        assertThat(saved.getIllegalBuildingStatus()).isEqualTo(IllegalBuildingStatus.NO);
        assertThat(saved.getDescription()).isEqualTo("room condition is excellent");
        assertThat(saved.isSold()).isFalse();
        assertThat(saved.getLoanProducts()).containsExactly(LoanProduct.KAKAO_BANK, LoanProduct.HF_YOUTH);
    }

    @Test
    @DisplayName("상세 조회는 생성된 확장 필드를 그대로 반환한다")
    void getByIdForEditReturnsExtendedFields() {
        CreateListingResponse created = listingService.create(new CreateListingRequest(
                "Seoul Seocho Banpo-daero 1",
                null,
                ParkingStatus.AVAILABLE,
                ElevatorStatus.NO,
                PetPolicy.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                LocalDate.of(2026, 2, 14),
                10000000L,
                0L,
                false,
                false,
                null,
                MoveInType.FIXED,
                new BigDecimal("14.20"),
                LocalDate.of(2021, 7, 15),
                10,
                4,
                8,
                60000L,
                LoanStatus.NONE,
                IllegalBuildingStatus.NO,
                "quiet neighborhood"
        ));

        ListingDetailResponse detail = listingService.getByIdForEdit(created.id());

        assertThat(detail.exclusivityArea()).isEqualByComparingTo(new BigDecimal("14.20"));
        assertThat(detail.useAprDay()).isEqualTo(LocalDate.of(2021, 7, 15));
        assertThat(detail.totalFloors()).isEqualTo(10);
        assertThat(detail.currentFloor()).isEqualTo(4);
        assertThat(detail.parkingCount()).isEqualTo(8);
        assertThat(detail.maintenanceFee()).isEqualTo(60000L);
        assertThat(detail.loanStatus()).isEqualTo(LoanStatus.NONE);
        assertThat(detail.illegalBuildingStatus()).isEqualTo(IllegalBuildingStatus.NO);
        assertThat(detail.description()).isEqualTo("quiet neighborhood");
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
                MoveInType.FIXED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Test"
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
                MoveInType.FIXED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Test"
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
                MoveInType.FIXED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Test"
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.getLatitude()).isEqualTo(37.5555);
        assertThat(patched.getLongitude()).isEqualTo(126.9780);
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
                MoveInType.FIXED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Test"
        ));

        Listing patched = listingRepository.findById(created.id()).orElseThrow();
        assertThat(patched.getImages()).hasSize(1);
        verify(imageStorageService).delete(List.of("listings/remove.jpg"));
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
    @DisplayName("입주 옵션이 지정날짜인데 입주 가능일이 없으면 400 에러가 발생된다")
    void createRejectsFixedWithoutMoveInDate() {
        CreateListingRequest request = new CreateListingRequest(
                "Seoul Gangnam Teheran-ro 1",
                null,
                ParkingStatus.AVAILABLE,
                ElevatorStatus.YES,
                PetPolicy.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                null,
                10000000L,
                500000L,
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
        );

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("입주 옵션이 즉시인데 입주 가능일이 존재하면 400 에러가 발생한다")
    void createRejectsImmediateWithMoveInDate() {
        CreateListingRequest request = new CreateListingRequest(
                "Seoul Gangnam Teheran-ro 1",
                null,
                ParkingStatus.AVAILABLE,
                ElevatorStatus.YES,
                PetPolicy.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                LocalDate.now(),
                10000000L,
                500000L,
                false,
                false,
                null,
                MoveInType.IMMEDIATE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
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
                ParkingStatus.AVAILABLE,
                ElevatorStatus.NO,
                PetPolicy.AVAILABLE,
                ContractType.JEONSE,
                RoomType.ONE_ROOM,
                List.of(LoanProduct.HF_YOUTH),
                LocalDate.now(),
                10000000L,
                0L,
                sold,
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
