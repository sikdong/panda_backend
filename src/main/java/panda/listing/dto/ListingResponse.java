package panda.listing.dto;

import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

import java.util.List;

public record ListingResponse(
        Long id,
        String address,
        Long deposit,
        Long monthlyRent,
        Long viewCount,
        List<LoanProduct> loanProducts,
        Boolean sold,
        Boolean hotProperty,
        RoomType roomType,
        Double latitude,
        Double longitude
) {
}
