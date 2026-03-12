package panda.listing.dto;

import java.math.BigDecimal;
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
        BigDecimal exclusivityArea,
        LocalDate useAprDay,
        Integer totalFloors,
        Integer currentFloor,
        Integer parkingCount,
        Long maintenanceFee,
        LoanStatus loanStatus,
        IllegalBuildingStatus illegalBuildingStatus,
        Long viewCount,
        Boolean hotProperty,
        Boolean recentlyRegistered,
        List<String> imagePaths,
        List<String> imageFilePaths,
        String description
) {
}
