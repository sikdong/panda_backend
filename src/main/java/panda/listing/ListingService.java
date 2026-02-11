package panda.listing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import panda.image.ImageStorageService;
import panda.listing.dto.CreateListingRequest;
import panda.listing.dto.CreateListingResponse;
import panda.listing.dto.ListingDetailResponse;
import panda.listing.dto.ListingResponse;
import panda.listing.dto.UpdateListingRequest;
import panda.listing.dto.UpdateListingSoldRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {

    private static final DateTimeFormatter MOVE_IN_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ListingRepository listingRepository;
    private final GeocodingService geocodingService;
    private final ImageStorageService imageStorageService;

    @Transactional
    public CreateListingResponse create(CreateListingRequest request) {
        return create(request, Collections.emptyList());
    }

    @Transactional
    public CreateListingResponse create(CreateListingRequest request, List<MultipartFile> imageFiles) {
        Coordinate coordinate = geocodingService.convertAddressToCoordinate(request.address());
        LocalDate moveInDate = parseMoveInDate(request.moveInDate());

        Listing listing = Listing.builder()
                .address(request.address().trim())
                .note(request.note())
                .parking(request.parking())
                .elevator(request.elevator())
                .pet(request.pet())
                .contractType(request.contractType())
                .roomType(request.roomType())
                .loanProducts(request.loanProducts())
                .moveInDate(moveInDate)
                .deposit(request.deposit())
                .monthlyRent(request.monthlyRent())
                .sold(Boolean.TRUE.equals(request.sold()))
                .latitude(coordinate.latitude())
                .longitude(coordinate.longitude())
                .build();

        List<String> imagePaths = imageStorageService.store(imageFiles);
        imagePaths.forEach(listing::addImagePath);

        Listing saved = listingRepository.save(listing);
        return new CreateListingResponse(saved.getId(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getSummaries() {
        return listingRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getUnsoldListings() {
        return listingRepository.findBySoldFalseOrderByUpdatedAtDescIdDesc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> searchByAddress(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address keyword must not be blank");
        }
        return listingRepository.findByAddressContainingIgnoreCaseOrderByUpdatedAtDescIdDesc(keyword.trim()).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public ListingDetailResponse getById(Long id) {
        Listing listing = findByIdOrThrow(id);
        listing.increaseViewCount();
        return toDetailResponse(listing);
    }

    @Transactional
    public void patch(Long id, UpdateListingRequest request) {
        patch(id, request, null);
    }

    @Transactional
    public void patch(Long id, UpdateListingRequest request, List<MultipartFile> images) {
        Listing listing = findByIdOrThrow(id);

        String address = listing.getAddress();
        Double latitude = listing.getLatitude();
        Double longitude = listing.getLongitude();

        if (request.address() != null) {
            if (request.address().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address must not be blank");
            }
            String trimmedAddress = request.address().trim();
            address = trimmedAddress;
            if (!trimmedAddress.equals(listing.getAddress())) {
                Coordinate coordinate = geocodingService.convertAddressToCoordinate(trimmedAddress);
                latitude = coordinate.latitude();
                longitude = coordinate.longitude();
            }
        }

        LocalDate moveInDate = listing.getMoveInDate();
        if (request.moveInDate() != null) {
            moveInDate = parseMoveInDate(request.moveInDate());
        }

        listing.updateDetails(
                address,
                request.note() != null ? request.note() : listing.getNote(),
                request.parking() != null ? request.parking() : listing.getParking(),
                request.elevator() != null ? request.elevator() : listing.getElevator(),
                request.pet() != null ? request.pet() : listing.getPet(),
                request.contractType() != null ? request.contractType() : listing.getContractType(),
                request.roomType() != null ? request.roomType() : listing.getRoomType(),
                request.loanProducts() != null ? request.loanProducts() : listing.getLoanProducts(),
                moveInDate,
                request.deposit() != null ? request.deposit() : listing.getDeposit(),
                request.monthlyRent() != null ? request.monthlyRent() : listing.getMonthlyRent(),
                request.sold() != null ? request.sold() : listing.isSold(),
                latitude,
                longitude
        );

        List<MultipartFile> safeImages = images == null ? List.of() : images;
        syncExistingImages(listing, request.imagePaths());
        List<String> newImagePaths = imageStorageService.store(safeImages);
        newImagePaths.forEach(listing::addImagePath);
    }

    @Transactional
    public void delete(Long id) {
        Listing listing = findByIdOrThrow(id);
        imageStorageService.delete(
                listing.getImages().stream()
                        .map(image -> image.getImagePath())
                        .toList()
        );
        listingRepository.delete(listing);
    }

    @Transactional
    public void patchSold(Long id, UpdateListingSoldRequest request) {
        Listing listing = findByIdOrThrow(id);
        listing.updateSold(request.sold());
    }

    private LocalDate parseMoveInDate(String moveInDate) {
        if (moveInDate == null || moveInDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(moveInDate.trim(), MOVE_IN_DATE_FORMATTER);
    }

    private Listing findByIdOrThrow(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found: " + id));
    }

    private ListingDetailResponse toDetailResponse(Listing listing) {
        return new ListingDetailResponse(
                listing.getAddress(),
                listing.getNote(),
                listing.getParking(),
                listing.getElevator(),
                listing.getPet(),
                listing.getContractType(),
                listing.getRoomType(),
                listing.getLoanProducts(),
                listing.getMoveInDate(),
                listing.getDeposit(),
                listing.getMonthlyRent(),
                listing.getViewCount(),
                listing.getImages().stream()
                        .map(image -> imageStorageService.issuePresignedGetUrl(image.getImagePath()))
                        .toList(),
                listing.getImages().stream()
                        .map(image -> image.getImagePath())
                        .toList()
        );
    }

    private ListingResponse toSummaryResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getAddress(),
                listing.getDeposit(),
                listing.getMonthlyRent(),
                listing.getViewCount(),
                listing.getLoanProducts(),
                listing.isSold(),
                listing.getRoomType(),
                listing.getLatitude(),
                listing.getLongitude()
        );
    }

    private void syncExistingImages(Listing listing, List<String> requestedImagePaths) {
        //FIXME 다시 구현
        if (requestedImagePaths == null) {
            return;
        }

        List<String> normalizedRequestedPaths = requestedImagePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(String::trim)
                .toList();

        Map<String, panda.image.Image> existingByPath = listing.getImages().stream()
                .collect(Collectors.toMap(panda.image.Image::getImagePath, Function.identity(), (left, right) -> left));
        Set<String> requestedPathSet = new LinkedHashSet<>(normalizedRequestedPaths);

        if (!existingByPath.keySet().containsAll(requestedPathSet)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown retained image path included");
        }

        List<String> pathsToDelete = existingByPath.keySet().stream()
                .filter(path -> !requestedPathSet.contains(path))
                .toList();
        if (!pathsToDelete.isEmpty()) {
            imageStorageService.delete(pathsToDelete);
        }

        listing.getImages().removeIf(image -> !requestedPathSet.contains(image.getImagePath()));

        Map<String, Integer> order = new java.util.HashMap<>();
        for (int i = 0; i < normalizedRequestedPaths.size(); i++) {
            order.putIfAbsent(normalizedRequestedPaths.get(i), i);
        }
        listing.getImages().sort((left, right) -> {
            int leftOrder = order.getOrDefault(left.getImagePath(), Integer.MAX_VALUE);
            int rightOrder = order.getOrDefault(right.getImagePath(), Integer.MAX_VALUE);
            return Integer.compare(leftOrder, rightOrder);
        });
    }
}
