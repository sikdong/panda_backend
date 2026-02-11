package panda.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class ImageStorageServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String PREFIX = "listings";
    private static final long EXPIRATION_SECONDS = 900;

    @Test
    @DisplayName("이미지 저장 시 S3에 업로드되고 저장된 키 목록이 반환된다")
    void storeSavesFileAndReturnsObjectKey() {
        S3Client s3Client = mockS3Client();
        S3Presigner s3Presigner = mockPresigner();
        ImageStorageService service = newService(s3Client, s3Presigner);
        MockMultipartFile file = imageFile("room.jpg", "sample".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        List<String> stored = service.store(List.of(file));

        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst()).startsWith(PREFIX + "/");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("이미지 저장 시 null 또는 빈 파일은 저장 대상에서 제외된다")
    void storeSkipsNullAndEmptyFiles() {
        ImageStorageService service = newService(mockS3Client(), mockPresigner());
        MockMultipartFile empty = imageFile("empty.jpg", new byte[0]);

        List<String> stored = service.store(Arrays.asList(null, empty));

        assertThat(stored).isEmpty();
    }

    @Test
    @DisplayName("Presigned URL 발급 시 S3 서명 URL 문자열이 반환된다")
    void issuePresignedGetUrlReturnsSignedUrl() {
        S3Client s3Client = mockS3Client();
        S3Presigner s3Presigner = mockPresigner();
        ImageStorageService service = newService(s3Client, s3Presigner);
        PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
        when(presigned.url()).thenReturn(toUrl("https://example-bucket.s3.ap-northeast-2.amazonaws.com/a.jpg"));

        String url = service.issuePresignedGetUrl("a.jpg");

        assertThat(url).contains("https://example-bucket.s3.ap-northeast-2.amazonaws.com/a.jpg");
    }

    @Test
    @DisplayName("Presigned URL 발급 시 경로 탐색 문자열이 포함되면 예외가 발생한다")
    void issuePresignedGetUrlRejectsPathTraversal() {
        ImageStorageService service = newService(mockS3Client(), mockPresigner());

        assertThatThrownBy(() -> service.issuePresignedGetUrl("../outside.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid image path");
    }

    @Test
    @DisplayName("이미지 삭제 요청 시 전달된 각 키에 대해 S3 삭제가 호출된다")
    void deleteRemovesObjectsFromS3() {
        S3Client s3Client = mockS3Client();
        ImageStorageService service = newService(s3Client, mockPresigner());

        service.delete(List.of("listings/a.png", "b.png"));

        verify(s3Client, org.mockito.Mockito.times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    private ImageStorageService newService(S3Client s3Client, S3Presigner s3Presigner) {
        return new ImageStorageService(s3Client, s3Presigner, BUCKET, PREFIX, EXPIRATION_SECONDS);
    }

    private S3Client mockS3Client() {
        return org.mockito.Mockito.mock(S3Client.class);
    }

    private S3Presigner mockPresigner() {
        return org.mockito.Mockito.mock(S3Presigner.class);
    }

    private MockMultipartFile imageFile(String name, byte[] bytes) {
        return new MockMultipartFile("image", name, "image/jpeg", bytes);
    }

    private URL toUrl(String value) {
        try {
            return new URL(value);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL for test", e);
        }
    }
}