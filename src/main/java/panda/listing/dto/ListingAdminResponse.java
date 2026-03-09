package panda.listing.dto;

import panda.listing.Listing;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.RoomType;

import java.util.List;

public record ListingAdminResponse(
        Long id,
        String address,
        Long deposit,
        Long monthlyRent,
        Long viewCount,
        Boolean sold,
        Boolean hotProperty,
        String description
) {
    public static ListingAdminResponse toAdminResponse(Listing listing) {
        return new ListingAdminResponse(
                listing.getId(),
                listing.getAddress(),
                listing.getDeposit(),
                listing.getMonthlyRent(),
                listing.getViewCount(),
                listing.isSold(),
                listing.isHotProperty(),
                listing.getDescription()
        );
    }
}
