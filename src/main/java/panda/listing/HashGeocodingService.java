package panda.listing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HashGeocodingService implements GeocodingService {

    private final String seed;

    public HashGeocodingService(@Value("${app.geocoding.seed:panda-map-seed}") String seed) {
        this.seed = seed;
    }

    @Override
    public Coordinate convertAddressToCoordinate(String address) {
        byte[] hash = sha256(seed + "|" + address.trim());

        long latValue = toPositiveLong(hash, 0);
        long lngValue = toPositiveLong(hash, 8);

        double latitude = 33.0 + (latValue / (double) Long.MAX_VALUE) * 5.5;
        double longitude = 124.0 + (lngValue / (double) Long.MAX_VALUE) * 8.5;

        return new Coordinate(round(latitude), round(longitude));
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private long toPositiveLong(byte[] bytes, int offset) {
        long value = 0;
        for (int i = offset; i < offset + 8; i++) {
            value = (value << 8) | (bytes[i] & 0xffL);
        }
        return value & Long.MAX_VALUE;
    }

    private double round(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }
}
