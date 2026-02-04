package panda.image;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class ImageStorageService {

    private static final String IMAGE_API_PREFIX = "/api/images/";
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String keyPrefix;
    private final long presignedGetExpirationSeconds;

    public ImageStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${app.image.s3.bucket}") String bucket,
            @Value("${app.image.s3.key-prefix:listings}") String keyPrefix,
            @Value("${app.image.s3.presigned-get-expiration-seconds:900}") long presignedGetExpirationSeconds
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket must be configured");
        }
        this.bucket = bucket.trim();
        this.keyPrefix = normalizePrefix(keyPrefix);
        this.presignedGetExpirationSeconds = presignedGetExpirationSeconds;
    }

    public List<String> store(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> imageKeys = new ArrayList<>();
        for (MultipartFile imageFile : imageFiles) {
            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            String key = save(imageFile);
            imageKeys.add(key);
        }
        return imageKeys;
    }

    public String issuePresignedGetUrl(String imagePathOrKey) {
        String key = normalizeToKey(imagePathOrKey);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofSeconds(presignedGetExpirationSeconds))
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public void delete(List<String> imagePathOrKeys) {
        if (imagePathOrKeys == null || imagePathOrKeys.isEmpty()) {
            return;
        }

        for (String imagePathOrKey : imagePathOrKeys) {
            String key = normalizeToKey(imagePathOrKey);
            try {
                DeleteObjectRequest request = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                s3Client.deleteObject(request);
            } catch (S3Exception ex) {
                throw new IllegalStateException("Failed to delete image file from S3", ex);
            }
        }
    }

    private String save(MultipartFile imageFile) {
        String extension = extractExtension(imageFile.getOriginalFilename());
        String fileName = UUID.randomUUID() + extension;
        String key = keyPrefix + "/" + fileName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(imageFile.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(imageFile.getBytes()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read image file", ex);
        } catch (S3Exception ex) {
            throw new IllegalStateException("Failed to upload image file to S3", ex);
        }
        return key;
    }

    private String normalizeToKey(String imagePathOrKey) {
        if (imagePathOrKey == null || imagePathOrKey.isBlank()) {
            throw new IllegalArgumentException("Invalid image path");
        }

        String key = imagePathOrKey.trim();
        if (key.startsWith(IMAGE_API_PREFIX)) {
            key = key.substring(IMAGE_API_PREFIX.length());
        }
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (!key.contains("/")) {
            key = keyPrefix + "/" + key;
        }
        if (key.contains("\\") || key.contains("..") || key.endsWith("/") || key.isBlank()) {
            throw new IllegalArgumentException("Invalid image path");
        }
        return key;
    }

    private String normalizePrefix(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("S3 key prefix must be configured");
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty() || normalized.contains("\\") || normalized.contains("..")) {
            throw new IllegalStateException("Invalid S3 key prefix");
        }
        return normalized;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dotIndex);
        return extension.matches("\\.[A-Za-z0-9]{1,10}") ? extension : "";
    }
}
