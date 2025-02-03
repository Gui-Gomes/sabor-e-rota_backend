package net.guilhermegomes.backend.controller;

import net.guilhermegomes.backend.domain.restaurant.Restaurant;
import net.guilhermegomes.backend.service.RestaurantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantsController {

    private final RestaurantsService restaurantsService;

    @Autowired
    public RestaurantsController(RestaurantsService restaurantsService) {
        this.restaurantsService = restaurantsService;
    }

    /**
     * Endpoint to search for restaurants near the specified geographic coordinates.
     *
     * @param latitude  The latitude of the search location.
     * @param longitude The longitude of the search location.
     * @return A list of restaurants within the defined radius.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> searchRestaurants(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<Restaurant> restaurants = restaurantsService.searchRestaurants(latitude, longitude);
        return ResponseEntity.ok(restaurants);
    }
}