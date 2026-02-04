package panda.listing.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateListingSoldRequest(
        @NotNull Boolean sold
) {
}
