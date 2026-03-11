package panda.image;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import panda.image.dto.IssuePresignedUploadUrlsRequest;
import panda.image.dto.PresignedUploadUrlResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageStorageService imageStorageService;

    @PostMapping("/presigned-urls")
    public List<PresignedUploadUrlResponse> issuePresignedUploadUrls(
            @Valid @RequestBody IssuePresignedUploadUrlsRequest request
    ) {
        return request.files().stream()
                .map(file -> {
                    String key = imageStorageService.createKey(request.listingId(), file.fileName());
                    String putUrl = imageStorageService.issuePresignedPutUrl(key, file.contentType());
                    return new PresignedUploadUrlResponse(key, putUrl);
                })
                .toList();
    }
}
