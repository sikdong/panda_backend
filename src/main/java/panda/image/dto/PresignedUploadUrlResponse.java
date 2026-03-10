package panda.image.dto;

public record PresignedUploadUrlResponse(
        String key,
        String putUrl
) {
}
