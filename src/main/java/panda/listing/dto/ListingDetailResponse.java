package panda.listing.dto;

import java.time.LocalDate;
import java.util.List;
import panda.listing.enums.AvailabilityStatus;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

public record ListingDetailResponse(
        String address,
        String note,
        AvailabilityStatus parking,
        ElevatorStatus elevator,
        AvailabilityStatus pet,
        ContractType contractType,
        RoomType roomType,
        List<LoanProduct> loanProducts,
        LocalDate moveInDate,
        Long deposit,
        Long monthlyRent,
        List<String> imagePaths,
        List<String> imageFilePaths
) {
}
