package panda.listing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import panda.analytics.AnalyticsService;
import panda.listing.dto.ListingDetailResponse;
import panda.listing.dto.ListingResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/map/listings")
public class MapListingController {

    private final ListingService listingService;
    private final ListingRecentViewedService listingRecentViewedService;
    private final AnalyticsService analyticsService;

    @GetMapping("/unsold")
    public List<ListingResponse> getUnsoldListings(HttpServletRequest request, HttpServletResponse response) {
        String actorKey = analyticsService.getOrCreateActorKey(request, response);
        analyticsService.trackVisit(actorKey, request.getRequestURI());
        return listingService.getUnsoldListings();
    }

    @GetMapping("/{id:\\d+}")
    public ListingDetailResponse getById(@PathVariable Long id) {
        return listingService.getByIdForView(id);
    }

    @PostMapping("/{id:\\d+}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordRecentViewed(@PathVariable Long id, HttpServletRequest request) {
        listingRecentViewedService.recordView(id, request);
    }

    @GetMapping("/recent-viewed")
    public List<ListingResponse> getRecentViewed(
            HttpServletRequest request,
            @RequestParam(required = false) Integer limit
    ) {
        return listingRecentViewedService.getRecentViewed(request, limit);
    }
}
