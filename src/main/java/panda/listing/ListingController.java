package panda.listing;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import panda.listing.dto.CreateListingRequest;
import panda.listing.dto.CreateListingResponse;
import panda.listing.dto.ListingDetailResponse;
import panda.listing.dto.ListingResponse;
import panda.listing.dto.UpdateListingRequest;
import panda.listing.dto.UpdateListingSoldRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/listings")
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
    public List<ListingResponse> getSummaries() {
        return listingService.getSummaries();
    }

    @GetMapping("/unsold")
    public List<ListingResponse> getUnsoldListings() {
        return listingService.getUnsoldListings();
    }

    @GetMapping("/search")
    public List<ListingResponse> searchByAddress(@RequestParam("address") String address) {
        return listingService.searchByAddress(address);
    }

    @GetMapping("/{id:\\d+}")
    public ListingDetailResponse getById(@PathVariable Long id) {
        return listingService.getById(id);
    }

    @PatchMapping(path = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> patch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        listingService.patch(id, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(path = "/{id:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> patchWithImages(
            @PathVariable Long id,
            @Valid @RequestPart("listing") UpdateListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        listingService.patch(id, request, images);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        listingService.delete(id);
    }

    @PatchMapping("/{id:\\d+}/sold")
    public ResponseEntity<Void> patchSold(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingSoldRequest request
    ) {
        listingService.patchSold(id, request);
        return ResponseEntity.ok().build();
    }

}
