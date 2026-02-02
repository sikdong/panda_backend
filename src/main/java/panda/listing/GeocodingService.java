package panda.listing;

public interface GeocodingService {
    Coordinate convertAddressToCoordinate(String address);
}
