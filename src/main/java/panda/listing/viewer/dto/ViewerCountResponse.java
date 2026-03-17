package panda.listing.viewer.dto;

public record ViewerCountResponse(
        Long listingId,
        int viewerCount
) {
}
