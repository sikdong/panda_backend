package panda.access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MapAccessService {

    private final String password;
    private final Duration tokenTtl;
    private final ConcurrentHashMap<String, Instant> tokens = new ConcurrentHashMap<>();

    public MapAccessService(
            @Value("${app.map-access.password:}") String password,
            @Value("${app.map-access.token-ttl-minutes:480}") long tokenTtlMinutes
    ) {
        this.password = password;
        this.tokenTtl = Duration.ofMinutes(tokenTtlMinutes);
    }

    public String issueToken(String submittedPassword) {
        if (password.isBlank()) {
            throw new MapAccessNotConfiguredException();
        }

        if (!MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8),
                submittedPassword.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidMapAccessPasswordException();
        }

        String token = UUID.randomUUID().toString();
        tokens.put(token, Instant.now().plus(tokenTtl));
        return token;
    }

    public boolean isAuthorized(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isBefore(expiresAt)) {
            return true;
        }
        tokens.remove(token, expiresAt);
        return false;
    }

    public static class InvalidMapAccessPasswordException extends RuntimeException {
    }

    public static class MapAccessNotConfiguredException extends RuntimeException {
    }
}
