package panda.listing.viewer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import panda.listing.Listing;
import panda.listing.ListingRepository;
import panda.listing.enums.ContractType;
import panda.listing.enums.ElevatorStatus;
import panda.listing.enums.LoanProduct;
import panda.listing.enums.MoveInType;
import panda.listing.enums.ParkingStatus;
import panda.listing.enums.PetPolicy;
import panda.listing.enums.RoomType;
import panda.listing.viewer.dto.ViewerPresenceRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ListingViewerPresenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long listing1Id;
    private Long listing2Id;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
        listing1Id = listingRepository.save(createListing("Seoul listing 1")).getId();
        listing2Id = listingRepository.save(createListing("Seoul listing 2")).getId();
    }

    @Test
    @DisplayName("같은 세션이 같은 매물에 중복 입장해도 viewerCount는 증가하지 않는다")
    void duplicateEnterDoesNotIncreaseViewerCount() throws Exception {
        String body = objectMapper.writeValueAsString(new ViewerPresenceRequest("session-a"));

        mockMvc.perform(post("/api/v1/listings/{id}/viewer-presence", listing1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingId").value(listing1Id))
                .andExpect(jsonPath("$.viewerCount").value(1));

        mockMvc.perform(post("/api/v1/listings/{id}/viewer-presence", listing1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(1));
    }

    @Test
    @DisplayName("입장-조회-이탈 흐름에서 viewerCount가 올바르게 변한다")
    void enterGetCountLeaveFlowWorks() throws Exception {
        mockMvc.perform(post("/api/v1/listings/{id}/viewer-presence", listing1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ViewerPresenceRequest("session-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(1));

        mockMvc.perform(post("/api/v1/listings/{id}/viewer-presence", listing1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ViewerPresenceRequest("session-b"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(2));

        mockMvc.perform(get("/api/v1/listings/{id}/viewer-count", listing1Id)
                        .param("viewerSessionId", "session-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingId").value(listing1Id))
                .andExpect(jsonPath("$.viewerCount").value(2));

        mockMvc.perform(delete("/api/v1/listings/{id}/viewer-presence", listing1Id)
                        .param("viewerSessionId", "session-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(1));
    }

    @Test
    @DisplayName("같은 세션이 다른 매물로 이동하면 이전 매물에서 제거되고 새 매물에만 남는다")
    void enteringAnotherListingMovesSession() throws Exception {
        mockMvc.perform(post("/api/v1/listings/{id}/viewer-presence", listing1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ViewerPresenceRequest("session-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(1));

        mockMvc.perform(post("/api/v1/listings/{id}/viewer-presence", listing2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ViewerPresenceRequest("session-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(1));

        mockMvc.perform(get("/api/v1/listings/{id}/viewer-count", listing1Id)
                        .param("viewerSessionId", "session-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(0));

        mockMvc.perform(get("/api/v1/listings/{id}/viewer-count", listing2Id)
                        .param("viewerSessionId", "session-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(1));
    }

    private Listing createListing(String address) {
        return Listing.builder()
                .address(address)
                .note(null)
                .description(null)
                .parking(ParkingStatus.AVAILABLE)
                .elevator(ElevatorStatus.NO)
                .pet(PetPolicy.AVAILABLE)
                .contractType(ContractType.JEONSE)
                .roomType(RoomType.ONE_ROOM)
                .loanProducts(List.of(LoanProduct.HF_YOUTH))
                .moveInType(MoveInType.FIXED)
                .moveInDate(null)
                .deposit(10_000L)
                .monthlyRent(0L)
                .latitude(37.5555)
                .longitude(126.9780)
                .build();
    }
}
