package panda.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import panda.listing.enums.AvailabilityStatus;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

public record UpdateListingRequest(
        @Size(max = 200) String address,
        @Size(max = 300) String note,
        AvailabilityStatus parking,
        ElevatorStatus elevator,
        AvailabilityStatus pet,
        ContractType contractType,
        RoomType roomType,
        List<LoanProduct> loanProducts,
        String moveInDate,
        @Min(0) Long deposit,
        @Min(0) Long monthlyRent,
        Boolean sold,
        Boolean hotProperty,
        List<@Size(max = 500) String> imagePaths
) {
}
