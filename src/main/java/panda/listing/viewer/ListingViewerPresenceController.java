package panda.listing.viewer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import panda.listing.ListingRepository;
import panda.listing.viewer.dto.ViewerCountResponse;
import panda.listing.viewer.dto.ViewerPresenceRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/listings")
public class ListingViewerPresenceController {

    private final ListingRepository listingRepository;
    private final ListingViewerPresenceService listingViewerPresenceService;

    @PostMapping("/{listingId:\\d+}/viewer-presence")
    @ResponseStatus(HttpStatus.OK)
    public ViewerCountResponse enter(
            @PathVariable Long listingId,
            @Valid @RequestBody ViewerPresenceRequest request
    ) {
        validateListingExists(listingId);
        int count = listingViewerPresenceService.enter(listingId, request.viewerSessionId());
        return new ViewerCountResponse(listingId, count);
    }

    @GetMapping("/{listingId:\\d+}/viewer-count")
    public ViewerCountResponse getViewerCount(
            @PathVariable Long listingId,
            @RequestParam(required = false) String viewerSessionId
    ) {
        validateListingExists(listingId);
        int count = listingViewerPresenceService.getViewerCount(listingId, viewerSessionId);
        return new ViewerCountResponse(listingId, count);
    }

    @DeleteMapping("/{listingId:\\d+}/viewer-presence")
    public ViewerCountResponse leave(
            @PathVariable Long listingId,
            @RequestParam String viewerSessionId
    ) {
        validateListingExists(listingId);
        int count = listingViewerPresenceService.leave(listingId, viewerSessionId);
        return new ViewerCountResponse(listingId, count);
    }

    private void validateListingExists(Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found: " + listingId);
        }
    }
}
