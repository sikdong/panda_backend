package panda.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private static final String IMAGE_API_PREFIX = "/api/images/";
    private final Path uploadDirectory;

    public ImageStorageService(@Value("${app.image.upload-dir:uploads/listings}") String uploadDir) {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create image upload directory", ex);
        }
    }

    public List<String> store(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> imagePaths = new ArrayList<>();
        for (MultipartFile imageFile : imageFiles) {
            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            String savedFileName = save(imageFile);
            imagePaths.add(IMAGE_API_PREFIX + savedFileName);
        }
        return imagePaths;
    }

    public Resource load(String fileName) {
        try {
            Path path = uploadDirectory.resolve(fileName).normalize();
            if (!path.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Invalid image path");
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Image not found");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load image file", ex);
        }
    }

    private String save(MultipartFile imageFile) {
        String extension = extractExtension(imageFile.getOriginalFilename());
        String savedFileName = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(savedFileName).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid image path");
        }

        try (InputStream inputStream = imageFile.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save image file", ex);
        }
        return savedFileName;
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
