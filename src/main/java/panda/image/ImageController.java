package panda.image;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import panda.image.dto.PresignedGetImageResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageStorageService imageStorageService;

    @GetMapping("/{fileName:.+}/presigned-get")
    public ResponseEntity<PresignedGetImageResponse> issuePresignedGet(@PathVariable String fileName) {
        try {
            String url = imageStorageService.issuePresignedGetUrl(fileName);
            return ResponseEntity.ok(new PresignedGetImageResponse(url));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/presigned-get", params = "key")
    public ResponseEntity<PresignedGetImageResponse> issuePresignedGetByKey(@RequestParam String key) {
        try {
            String url = imageStorageService.issuePresignedGetUrl(key);
            return ResponseEntity.ok(new PresignedGetImageResponse(url));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
