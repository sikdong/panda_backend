package panda.image.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record IssuePresignedUploadUrlsRequest(
        @NotNull @Positive Long listingId,
        @NotEmpty List<@Valid PresignedUploadFileRequest> files
) {
}
