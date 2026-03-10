package panda.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignedUploadFileRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 100) String contentType
) {
}
