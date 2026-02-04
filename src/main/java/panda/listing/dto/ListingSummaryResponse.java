package panda.listing.dto;

import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

import java.util.List;

public record ListingSummaryResponse(
        Long id,
        String address,
        Long deposit,
        Long monthlyRent,
        List<LoanProduct> loanProducts,
        boolean sold,
        RoomType roomType,
        Double latitude,
        Double longitude
) {
}
