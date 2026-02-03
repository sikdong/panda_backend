package panda.listing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import panda.image.ImageStorageService;
import panda.listing.dto.CreateListingRequest;
import panda.listing.dto.CreateListingResponse;
import panda.listing.dto.ListingSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {

    private static final DateTimeFormatter MOVE_IN_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ListingRepository listingRepository;
    private final GeocodingService geocodingService;
    private final ImageStorageService imageStorageService;

    @Transactional
    public CreateListingResponse create(CreateListingRequest request) {
        return create(request, Collections.emptyList());
    }

    @Transactional
    public CreateListingResponse create(CreateListingRequest request, List<MultipartFile> imageFiles) {
        Coordinate coordinate = geocodingService.convertAddressToCoordinate(request.address());

        Listing listing = Listing.builder()
                .address(request.address().trim())
                .note(request.note())
                .parking(request.parking())
                .elevator(request.elevator())
                .pet(request.pet())
                .contractType(request.contractType())
                .roomType(request.roomType())
                .loanProducts(request.loanProducts())
                .moveInDate(LocalDate.parse(request.moveInDate(), MOVE_IN_DATE_FORMATTER))
                .deposit(request.deposit())
                .monthlyRent(request.monthlyRent())
                .latitude(coordinate.latitude())
                .longitude(coordinate.longitude())
                .build();

        List<String> imagePaths = imageStorageService.store(imageFiles);
        imagePaths.forEach(listing::addImagePath);

        Listing saved = listingRepository.save(listing);
        return new CreateListingResponse(saved.getId(), saved.getCreatedAt());
    }

    public List<ListingSummaryResponse> getSummaries() {
        return listingRepository.findAll().stream()
                .map(listing -> new ListingSummaryResponse(
                        listing.getId(),
                        listing.getAddress(),
                        listing.getDeposit(),
                        listing.getMonthlyRent(),
                        listing.getContractType(),
                        listing.getLatitude(),
                        listing.getLongitude()
                ))
                .toList();
    }
}
