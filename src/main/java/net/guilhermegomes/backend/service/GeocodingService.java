package net.guilhermegomes.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.guilhermegomes.backend.dto.CoordinateDTO;
import net.guilhermegomes.backend.domain.address.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Service for interacting with the Google Geocoding API to retrieve address information based on geographic coordinates.
 * This service converts latitude and longitude coordinates into a human-readable address using the Google Geocoding API.
 */
@Service
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

    @Value("${API_KEY}")
    private String apiKey;

    private static final String GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ROUTE = "route";
    private static final String SUBLOCALITY = "sublocality";
    private static final String ADMIN_AREA_LEVEL_2 = "administrative_area_level_2";
    private static final String ADMIN_AREA_LEVEL_1 = "administrative_area_level_1";
    private static final String POSTAL_CODE = "postal_code";
    private static final String COUNTRY = "country";

    /**
     * Retrieves the address corresponding to the given geographic coordinates (latitude and longitude).
     * Makes a request to the Google Geocoding API and processes the response to extract address components.
     *
     * @param coordinate The CoordinateDTO containing latitude and longitude.
     * @return An Optional containing an Address object with the address details or an empty Optional if no address is found.
     */
    public Optional<Address> getAddressByCoordinate(CoordinateDTO coordinate) {
        double latitude = coordinate.getLatitude();
        double longitude = coordinate.getLongitude();
        String location = latitude + "," + longitude;

        logger.info("Fetching address for coordinates: lat={}, lon={}", latitude, longitude);

        String url = UriComponentsBuilder.fromHttpUrl(GEOCODING_URL)
                .queryParam("latlng", location)
                .queryParam("key", apiKey)
                .toUriString();

        try {
            logger.debug("Request URL: {}", url);
            String jsonResponse = restTemplate.getForObject(url, String.class);
            logger.debug("Response received from Google API: {}", jsonResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.get("results");

            if (results.isArray() && results.size() > 0) {
                JsonNode relevantAddress = results.get(0);
                Address address = parseAddressComponents(relevantAddress, latitude, longitude);
                logger.info("Address successfully retrieved: {}", address);
                return Optional.of(address);
            } else {
                logger.warn("No address found for the given coordinates: lat={}, lon={}", latitude, longitude);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error while fetching address from Google API: ", e);
            throw new RuntimeException("Error while fetching address from Google API.", e);
        }
    }

    /**
     * Parses the address components from the Google Geocoding API response.
     *
     * @param relevantAddress The JSON node containing the address components.
     * @param latitude        The latitude of the location.
     * @param longitude       The longitude of the location.
     * @return An Address object with the parsed address details.
     */
    private Address parseAddressComponents(JsonNode relevantAddress, double latitude, double longitude) {
        JsonNode addressComponents = relevantAddress.get("address_components");

        String street = "";
        String neighborhood = "";
        String city = "";
        String state = "";
        String postalCode = "";
        String country = "";

        for (JsonNode component : addressComponents) {
            for (JsonNode type : component.get("types")) {
                String componentType = type.asText();

                switch (componentType) {
                    case ROUTE:
                        street = component.get("long_name").asText();
                        break;
                    case SUBLOCALITY:
                        neighborhood = component.get("long_name").asText();
                        break;
                    case ADMIN_AREA_LEVEL_2:
                        city = component.get("long_name").asText();
                        break;
                    case ADMIN_AREA_LEVEL_1:
                        state = component.get("long_name").asText();
                        break;
                    case POSTAL_CODE:
                        postalCode = component.get("long_name").asText();
                        break;
                    case COUNTRY:
                        country = component.get("long_name").asText();
                        break;
                }
            }
        }

        return new Address(street, neighborhood, city, state, postalCode, country, latitude, longitude);
    }
}
