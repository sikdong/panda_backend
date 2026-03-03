package panda.listing.dto;

import java.time.LocalDate;
import java.util.List;

import panda.listing.enums.*;

public record ListingDetailResponse(
        String address,
        String note,
        ParkingStatus parking,
        ElevatorStatus elevator,
        PetPolicy pet,
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
