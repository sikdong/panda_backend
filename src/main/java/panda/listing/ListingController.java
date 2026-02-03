package panda.listing;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import panda.listing.dto.CreateListingRequest;
import panda.listing.dto.CreateListingResponse;
import panda.listing.dto.ListingSummaryResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateListingResponse create(@Valid @RequestBody CreateListingRequest request) {
        return listingService.create(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateListingResponse createWithImages(
            @Valid @RequestPart("listing") CreateListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return listingService.create(request, images);
    }

    @GetMapping("/summaries")
    public List<ListingSummaryResponse> getSummaries() {
        return listingService.getSummaries();
    }
}
