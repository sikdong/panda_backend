package panda.listing.dto;

import panda.listing.enums.ContractType;

public record ListingSummaryResponse(
        Long id,
        String address,
        Long deposit,
        Long monthlyRent,
        ContractType contractType,
        Double latitude,
        Double longitude
) {
}
