package panda.listing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import panda.image.Image;
import panda.image.ImageStorageService;
import panda.listing.dto.*;
import panda.listing.enums.MoveInType;

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
        Coordinate coordinate = geocodingService.convertAddressToCoordinate(request.address());
        LocalDate moveInDate = request.moveInDate();
        validateMoveInCombination(request.moveInType(), moveInDate);

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
                .exclusivityArea(request.exclusivityArea())
                .useAprDay(request.useAprDay())
                .totalFloors(request.totalFloors())
                .currentFloor(request.currentFloor())
                .parkingCount(request.parkingCount())
                .maintenanceFee(request.maintenanceFee())
                .loanStatus(request.loanStatus())
                .illegalBuildingStatus(request.illegalBuildingStatus())
                .description(request.description())
                .sold(Boolean.TRUE.equals(request.sold()))
                .hotProperty(Boolean.TRUE.equals(request.hotProperty()))
                .latitude(coordinate.latitude())
                .longitude(coordinate.longitude())
                .moveInType(request.moveInType())
                .build();

        Listing saved = listingRepository.save(listing);
        normalizeRequestedImagePaths(request.imagePaths()).forEach(saved::addImagePath);
        return new CreateListingResponse(saved.getId(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getSummaries() {
        return listingRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getUnsoldListings() {
        return listingRepository.findBySoldFalseOrderByUpdatedAtDesc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingAdminResponse> getAdminListings() {
        return listingRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(ListingAdminResponse::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> searchByAddress(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address keyword must not be blank");
        }
        return listingRepository.findByAddressContainingIgnoreCaseOrderByUpdatedAtDesc(keyword.trim()).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public ListingDetailResponse getByIdForView(Long id) {
        Listing listing = findByIdOrThrow(id);
        listing.increaseViewCount();
        return toDetailResponse(listing);
    }

    @Transactional(readOnly = true)
    public ListingDetailResponse getByIdForEdit(Long id) {
        Listing listing = findByIdOrThrow(id);
        return toDetailResponse(listing);
    }

    @Transactional
    public void patch(Long id, UpdateListingRequest request) {
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
        LocalDate moveInDate = request.moveInDate() != null
                ? request.moveInDate()
                : listing.getMoveInDate();
        MoveInType moveInType = request.moveInType() != null
                ? request.moveInType()
                : listing.getMoveInType();
        validateMoveInCombination(moveInType, moveInDate);

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
                request.exclusivityArea() != null ? request.exclusivityArea() : listing.getExclusivityArea(),
                request.useAprDay() != null ? request.useAprDay() : listing.getUseAprDay(),
                request.totalFloors() != null ? request.totalFloors() : listing.getTotalFloors(),
                request.currentFloor() != null ? request.currentFloor() : listing.getCurrentFloor(),
                request.parkingCount() != null ? request.parkingCount() : listing.getParkingCount(),
                request.maintenanceFee() != null ? request.maintenanceFee() : listing.getMaintenanceFee(),
                request.loanStatus() != null ? request.loanStatus() : listing.getLoanStatus(),
                request.illegalBuildingStatus() != null ? request.illegalBuildingStatus() : listing.getIllegalBuildingStatus(),
                request.sold() != null ? request.sold() : listing.isSold(),
                request.hotProperty() != null ? request.hotProperty() : listing.isHotProperty(),
                latitude,
                longitude,
                moveInType,
                request.description() != null ? request.description() : listing.getDescription()
        );

        syncExistingImages(listing, request.imagePaths());
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

    private void validateMoveInCombination(MoveInType moveInType, LocalDate moveInDate) {
        if (moveInType == MoveInType.FIXED && moveInDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moveInDate is required when moveInType is FIXED");
        }
        if (moveInType == MoveInType.IMMEDIATE && moveInDate != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moveInDate must be null when moveInType is IMMEDIATE");
        }
    }

    private List<String> normalizeRequestedImagePaths(List<String> requestedImagePaths) {
        if (requestedImagePaths == null || requestedImagePaths.isEmpty()) {
            return List.of();
        }

        return requestedImagePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(path -> {
                    try {
                        return imageStorageService.normalizeKey(path);
                    } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path included", ex);
                    }
                })
                .toList();
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
                listing.getMoveInType(),
                listing.getMoveInType() == null ? null : listing.getMoveInType().getLabel(),
                listing.getMoveInDate(),
                listing.getDeposit(),
                listing.getMonthlyRent(),
                listing.getExclusivityArea(),
                listing.getUseAprDay(),
                listing.getTotalFloors(),
                listing.getCurrentFloor(),
                listing.getParkingCount(),
                listing.getMaintenanceFee(),
                listing.getLoanStatus(),
                listing.getIllegalBuildingStatus(),
                listing.getViewCount(),
                listing.isHotProperty(),
                listing.getImages().stream()
                        .map(image -> imageStorageService.issuePresignedGetUrl(image.getImagePath()))
                        .toList(),
                listing.getImages().stream()
                        .map(image -> image.getImagePath())
                        .toList(),
                listing.getDescription()
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
                listing.isHotProperty(),
                listing.getRoomType(),
                listing.getLatitude(),
                listing.getLongitude()
        );
    }

    private void syncExistingImages(Listing listing, List<String> requestedImagePaths) {
        if (requestedImagePaths == null) {
            return;
        }

        List<String> normalizedRequestedPaths = normalizeRequestedImagePaths(requestedImagePaths);
        Map<String, Image> existingByPath = listing.getImages().stream()
                .collect(Collectors.toMap(Image::getImagePath, Function.identity(), (left, right) -> left));
        List<String> orderedRequestedPaths = new java.util.ArrayList<>(new LinkedHashSet<>(normalizedRequestedPaths));

        List<String> pathsToDelete = existingByPath.keySet().stream()
                .filter(path -> !orderedRequestedPaths.contains(path))
                .toList();
        if (!pathsToDelete.isEmpty()) {
            imageStorageService.delete(pathsToDelete);
        }

        listing.getImages().removeIf(image -> !orderedRequestedPaths.contains(image.getImagePath()));
        orderedRequestedPaths.stream()
                .filter(path -> !existingByPath.containsKey(path))
                .forEach(listing::addImagePath);

        Map<String, Integer> order = new java.util.HashMap<>();
        for (int i = 0; i < orderedRequestedPaths.size(); i++) {
            order.putIfAbsent(orderedRequestedPaths.get(i), i);
        }
        listing.getImages().sort((left, right) -> {
            int leftOrder = order.getOrDefault(left.getImagePath(), Integer.MAX_VALUE);
            int rightOrder = order.getOrDefault(right.getImagePath(), Integer.MAX_VALUE);
            return Integer.compare(leftOrder, rightOrder);
        });
    }
}
