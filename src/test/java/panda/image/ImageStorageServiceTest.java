package panda.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeSavesFileAndReturnsApiPath() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("image", "room.jpg", "image/jpeg", "sample".getBytes());

        List<String> stored = service.store(List.of(file));

        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst()).startsWith("/api/images/");

        String fileName = stored.getFirst().replace("/api/images/", "");
        assertThat(Files.exists(tempDir.resolve(fileName))).isTrue();

        Resource loaded = service.load(fileName);
        assertThat(loaded.exists()).isTrue();
    }

    @Test
    void storeSkipsNullAndEmptyFiles() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        MockMultipartFile empty = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        List<String> stored = service.store(Arrays.asList(null, empty));

        assertThat(stored).isEmpty();
    }

    @Test
    void loadRejectsPathTraversal() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());

        assertThatThrownBy(() -> service.load("../outside.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid image path");
    }

    @Test
    void loadThrowsWhenImageDoesNotExist() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());

        assertThatThrownBy(() -> service.load("missing.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image not found");
    }
}
