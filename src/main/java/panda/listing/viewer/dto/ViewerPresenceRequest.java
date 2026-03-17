package panda.listing.viewer.dto;

import jakarta.validation.constraints.NotBlank;

public record ViewerPresenceRequest(
        @NotBlank String viewerSessionId
) {
}
