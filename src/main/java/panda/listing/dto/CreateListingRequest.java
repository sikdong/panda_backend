package panda.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import panda.listing.enums.AvailabilityStatus;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

public record CreateListingRequest(
        @NotBlank @Size(max = 200) String address,
        @Size(max = 300) String note,
        @NotNull AvailabilityStatus parking,
        @NotNull ElevatorStatus elevator,
        @NotNull AvailabilityStatus pet,
        @NotNull ContractType contractType,
        @NotNull RoomType roomType,
        @NotEmpty List<LoanProduct> loanProducts,
        @NotBlank @Pattern(regexp = "\\d{8}") String moveInDate,
        @NotNull @Min(0) Long deposit,
        @NotNull @Min(0) Long monthlyRent
) {
}
