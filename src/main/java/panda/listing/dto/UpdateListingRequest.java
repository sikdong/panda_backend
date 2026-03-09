package panda.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import panda.listing.enums.*;

public record UpdateListingRequest(
        @Size(max = 200) String address,
        @Size(max = 300) String note,
        ParkingStatus parking,
        ElevatorStatus elevator,
        PetPolicy pet,
        ContractType contractType,
        RoomType roomType,
        List<LoanProduct> loanProducts,
        LocalDate moveInDate,
        @Min(0) Long deposit,
        @Min(0) Long monthlyRent,
        Boolean sold,
        Boolean hotProperty,
        List<@Size(max = 500) String> imagePaths,
        MoveInType moveInType,
        BigDecimal exclusivityArea,
        LocalDate useAprDay,
        Integer totalFloors,
        Integer currentFloor,
        Integer parkingCount,
        Long maintenanceFee,
        LoanStatus loanStatus,
        IllegalBuildingStatus illegalBuildingStatus,
        String description
) {
}
