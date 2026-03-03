package panda.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import panda.listing.enums.*;

public record CreateListingRequest(
        @NotBlank @Size(max = 200) String address,
        @Size(max = 300) String note,
        @NotNull ParkingStatus parking,
        @NotNull ElevatorStatus elevator,
        @NotNull PetPolicy pet,
        @NotNull ContractType contractType,
        @NotNull RoomType roomType,
        @NotEmpty List<LoanProduct> loanProducts,
        LocalDate moveInDate,
        @NotNull @Min(0) Long deposit,
        @NotNull @Min(0) Long monthlyRent,
        Boolean sold,
        Boolean hotProperty,
        MoveInType moveInType,
        BigDecimal exclusivityArea,
        LocalDate useAprDay,
        Integer totalFloors,
        Integer currentFloor,
        Integer parkingCount,
        Long maintenanceFee,
        LoanStatus loanStatus,
        IllegalBuildingStatus illegalBuildingStatus
) {
}
