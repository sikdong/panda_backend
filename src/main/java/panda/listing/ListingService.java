package panda.listing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public CreateListingResponse create(CreateListingRequest request) {
        Coordinate coordinate = geocodingService.convertAddressToCoordinate(request.address());

        Listing listing = new Listing();
        listing.setAddress(request.address().trim());
        listing.setNote(request.note());
        listing.setParking(request.parking());
        listing.setElevator(request.elevator());
        listing.setPet(request.pet());
        listing.setContractType(request.contractType());
        listing.setRoomType(request.roomType());
        listing.setLoanProduct(request.loanProduct());
        listing.setMoveInDate(LocalDate.parse(request.moveInDate(), MOVE_IN_DATE_FORMATTER));
        listing.setDeposit(request.deposit());
        listing.setMonthlyRent(request.monthlyRent());
        listing.setLatitude(coordinate.latitude());
        listing.setLongitude(coordinate.longitude());

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
