package panda.listing.dto;

import java.time.LocalDateTime;

public record CreateListingResponse(
        Long id,
        LocalDateTime createdAt
) {
}
