package panda.listing;

import jakarta.persistence.*;
import lombok.*;
import panda.image.Image;
import panda.listing.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "listings",
        indexes = {
                @Index(name = "idx_listings_updated_at_id", columnList = "updated_at, id")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(length = 300)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityStatus parking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElevatorStatus elevator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityStatus pet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomType roomType;

    @Convert(converter = LoanProductListConverter.class)
    @Column(name = "loan_product", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private List<LoanProduct> loanProducts = new ArrayList<>();

    @Column
    private LocalDate moveInDate;

    @Column(nullable = false)
    private Long deposit;

    @Column(nullable = false)
    private Long monthlyRent;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private boolean sold = false;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    public void addImagePath(String imagePath) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(Image.builder()
                .imagePath(imagePath)
                .listing(this)
                .build());
    }

    public void updateDetails(
            String address,
            String note,
            AvailabilityStatus parking,
            ElevatorStatus elevator,
            AvailabilityStatus pet,
            ContractType contractType,
            RoomType roomType,
            List<LoanProduct> loanProducts,
            LocalDate moveInDate,
            Long deposit,
            Long monthlyRent,
            boolean sold,
            Double latitude,
            Double longitude
    ) {
        this.address = address;
        this.note = note;
        this.parking = parking;
        this.elevator = elevator;
        this.pet = pet;
        this.contractType = contractType;
        this.roomType = roomType;
        this.loanProducts = loanProducts == null ? new ArrayList<>() : new ArrayList<>(loanProducts);
        this.moveInDate = moveInDate;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.sold = sold;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateSold(boolean sold) {
        this.sold = sold;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
