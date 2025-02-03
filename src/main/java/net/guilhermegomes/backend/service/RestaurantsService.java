package net.guilhermegomes.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.guilhermegomes.backend.domain.address.Address;
import net.guilhermegomes.backend.domain.restaurant.Restaurant;
import net.guilhermegomes.backend.domain.userQuery.UserQuery;
import net.guilhermegomes.backend.dto.CoordinateDTO;
import net.guilhermegomes.backend.repository.AddressRepository;
import net.guilhermegomes.backend.repository.RestaurantRepository;
import net.guilhermegomes.backend.repository.UserQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for interacting with the Google Places API to search for restaurants based on geographic coordinates.
 */
@Service
public class RestaurantsService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantsService.class);

    @Value("${API_KEY}")
    private String apiKey;

    private static final int RADIUS = 1000; // Search radius in meters (1km)
    private static final String PLACES_API_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    private static final String TYPE = "restaurant";

    private final RestTemplate restTemplate;
    private final GeocodingService geocodingService;
    private final AddressRepository addressRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserQueryRepository userQueryRepository;

    /**
     * Constructs a new RestaurantsService instance.
     *
     * @param restTemplate         The RestTemplate used to make HTTP requests.
     * @param geocodingService     The service responsible for geocoding addresses.
     * @param addressRepository    The repository for managing address data.
     * @param restaurantRepository The repository for managing restaurant data.
     * @param userQueryRepository The repository for managing user queries.
     */
    public RestaurantsService(RestTemplate restTemplate,
                              GeocodingService geocodingService,
                              AddressRepository addressRepository,
                              RestaurantRepository restaurantRepository,
                              UserQueryRepository userQueryRepository) {
        this.restTemplate = restTemplate;
        this.geocodingService = geocodingService;
        this.addressRepository = addressRepository;
        this.restaurantRepository = restaurantRepository;
        this.userQueryRepository = userQueryRepository;
    }

    /**
     * Searches for restaurants near the specified geographic coordinates (latitude and longitude).
     *
     * @param latitude  The latitude of the search location.
     * @param longitude The longitude of the search location.
     * @return A list of restaurants within the specified radius.
     */
    public List<Restaurant> searchRestaurants(double latitude, double longitude) {
        String location = latitude + "," + longitude;
        String url = buildUrl(location);

        logger.info("Initiating restaurant search for location: latitude={}, longitude={}", latitude, longitude);
        logger.debug("Constructed Places API request URL: {}", url);

        // Fetch the address for the user query
        CoordinateDTO queryCoordinate = new CoordinateDTO(latitude, longitude);
        Optional<Address> queryAddressOpt = geocodingService.getAddressByCoordinate(queryCoordinate);

        if (queryAddressOpt.isEmpty()) {
            logger.error("No address found for the provided coordinates: latitude={}, longitude={}", latitude, longitude);
            throw new IllegalArgumentException("Address not found for the provided coordinates.");
        }

        Address queryAddress = queryAddressOpt.get();

        // Save or retrieve the address from the repository
        Address existingAddress = addressRepository.findAddressByStreetCityAndPostalCode(
                queryAddress.getStreet(),
                queryAddress.getCity(),
                queryAddress.getPostalCode()
        ).orElseGet(() -> addressRepository.save(queryAddress));

        // Save the user query
        UserQuery userQuery = new UserQuery(existingAddress);
        userQueryRepository.save(userQuery);

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            logger.debug("Response received from Places API: {}", jsonResponse);

            // Parse and return the list of restaurants
            return parseAndReturnRestaurants(jsonResponse, latitude, longitude, existingAddress);

        } catch (Exception e) {
            logger.error("Error occurred while fetching restaurants from Places API for location: latitude={}, longitude={}",
                    latitude, longitude, e);
            throw new RuntimeException("Error while fetching restaurants from Places API.", e);
        }
    }

    /**
     * Constructs the URL to make a request to the Google Places API with the specified query parameters.
     *
     * @param location A string representing the geographic coordinates (latitude,longitude).
     * @return The constructed URL for the Places API request.
     */
    private String buildUrl(String location) {
        logger.debug("Building request URL for Places API with location: {}", location);
        return UriComponentsBuilder.fromHttpUrl(PLACES_API_URL)
                .queryParam("location", location)
                .queryParam("radius", RADIUS)
                .queryParam("type", TYPE)
                .queryParam("key", apiKey)
                .toUriString();
    }

    /**
     * Parses the raw JSON response from the Google Places API and extracts the restaurant details.
     * If the restaurant is not already in the database, it is saved. The method returns a list of restaurants.
     *
     * @param jsonResponse The raw JSON response from the API.
     * @param latitude     The latitude of the search location.
     * @param longitude    The longitude of the search location.
     * @param queryAddress The address of the original query location.
     * @return A list of restaurants extracted from the API response.
     * @throws Exception If an error occurs during JSON parsing or address retrieval.
     */
    private List<Restaurant> parseAndReturnRestaurants(String jsonResponse, double latitude, double longitude, Address queryAddress) throws Exception {
        logger.debug("Parsing response from Places API...");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode results = root.path("results");

        if (results.isEmpty()) {
            logger.warn("No restaurants found in the response from Places API.");
        }

        List<Restaurant> restaurants = new ArrayList<>();
        for (JsonNode result : results) {
            try {
                String name = result.path("name").asText();
                double rating = result.path("rating").asDouble();
                double restaurantLatitude = result.path("geometry").path("location").path("lat").asDouble();
                double restaurantLongitude = result.path("geometry").path("location").path("lng").asDouble();

                CoordinateDTO restaurantCoordinate = new CoordinateDTO(restaurantLatitude, restaurantLongitude);
                Optional<Address> restaurantAddressOpt = geocodingService.getAddressByCoordinate(restaurantCoordinate);

                if (restaurantAddressOpt.isEmpty()) {
                    logger.warn("No address found for restaurant: name={}", name);
                    continue;
                }

                Address restaurantAddress = restaurantAddressOpt.get();

                // Check if the restaurant's address already exists in the database
                Address existingAddress = addressRepository.findAddressByStreetCityAndPostalCode(
                        restaurantAddress.getStreet(),
                        restaurantAddress.getCity(),
                        restaurantAddress.getPostalCode()
                ).orElseGet(() -> addressRepository.save(restaurantAddress)); // Save if the address doesn't exist

                // Check if the restaurant already exists in the database
                List<Restaurant> existingRestaurants = restaurantRepository.findByNameAndFullAddress(
                        name, existingAddress.getStreet(), existingAddress.getCity(),
                        existingAddress.getState(), existingAddress.getPostalCode()
                );

                if (!existingRestaurants.isEmpty()) {
                    logger.info("Restaurant already exists: name={}, address={}", name, existingAddress);
                    restaurants.add(existingRestaurants.get(0)); // Add the first matching restaurant to the list
                } else {
                    // Save new restaurant
                    Restaurant restaurant = new Restaurant();
                    restaurant.setName(name);
                    restaurant.setRating(rating);
                    restaurant.setAddress(existingAddress);

                    restaurantRepository.save(restaurant);
                    restaurants.add(restaurant);

                    logger.info("Successfully saved new restaurant: {}", restaurant);
                }

            } catch (Exception e) {
                logger.error("Error occurred while processing a restaurant. Skipping this entry.", e);
            }
        }
        return restaurants;
    }
}
