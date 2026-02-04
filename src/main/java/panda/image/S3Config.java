package panda.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${app.image.s3.region:ap-northeast-2}") String region,
            @Value("${app.image.s3.credentials.access-key:}") String accessKey,
            @Value("${app.image.s3.credentials.secret-key:}") String secretKey,
            @Value("${app.image.s3.credentials.session-token:}") String sessionToken
    ) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider(accessKey, secretKey, sessionToken))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${app.image.s3.region:ap-northeast-2}") String region,
            @Value("${app.image.s3.credentials.access-key:}") String accessKey,
            @Value("${app.image.s3.credentials.secret-key:}") String secretKey,
            @Value("${app.image.s3.credentials.session-token:}") String sessionToken
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider(accessKey, secretKey, sessionToken))
                .build();
    }

    private StaticCredentialsProvider buildCredentialsProvider(String accessKey, String secretKey, String sessionToken) {
        String trimmedAccessKey = trim(accessKey);
        String trimmedSecretKey = trim(secretKey);
        String trimmedSessionToken = trim(sessionToken);

        if (isBlank(trimmedAccessKey) || isBlank(trimmedSecretKey)) {
            throw new IllegalStateException(
                    "AWS credentials are missing. Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY."
            );
        }

        if (!isBlank(trimmedSessionToken)) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(trimmedAccessKey, trimmedSecretKey, trimmedSessionToken)
            );
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(trimmedAccessKey, trimmedSecretKey));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
