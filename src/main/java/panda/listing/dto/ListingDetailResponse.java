package panda.listing.dto;

import java.time.LocalDate;
import java.util.List;
import panda.listing.enums.ParkingStatus;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.MoveInType;
import panda.listing.enums.RoomType;

public record ListingDetailResponse(
        String address,
        String note,
        ParkingStatus parking,
        ElevatorStatus elevator,
        ParkingStatus pet,
        ContractType contractType,
        RoomType roomType,
        List<LoanProduct> loanProducts,
        MoveInType moveInType,
        String moveInTypeLabel,
        LocalDate moveInDate,
        Long deposit,
        Long monthlyRent,
        Long viewCount,
        Boolean hotProperty,
        List<String> imagePaths,
        List<String> imageFilePaths
) {
}
