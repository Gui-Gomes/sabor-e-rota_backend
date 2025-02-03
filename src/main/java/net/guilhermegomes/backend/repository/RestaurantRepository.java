package net.guilhermegomes.backend.repository;

import net.guilhermegomes.backend.domain.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Restaurant entities.
 * Provides CRUD operations and custom queries for searching restaurants.
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * Finds a restaurant by its name.
     *
     * @param name The name of the restaurant.
     * @return An optional containing the found restaurant, if any.
     */
    Optional<Restaurant> findByName(String name);

    /**
     * Finds all restaurants by a given rating.
     *
     * @param rating The rating value to search for.
     * @return A list of restaurants with the specified rating.
     */
    List<Restaurant> findByRating(double rating);

    /**
     * Finds all restaurants in a specific city.
     *
     * @param city The city name to search for.
     * @return A list of restaurants in the specified city.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.address.city = :city")
    List<Restaurant> findByCity(@Param("city") String city);

    /**
     * Finds all restaurants in a specific state.
     *
     * @param state The state code to search for.
     * @return A list of restaurants in the specified state.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.address.state = :state")
    List<Restaurant> findByState(@Param("state") String state);

    /**
     * Finds all restaurants within a given rating range.
     *
     * @param minRating Minimum rating value.
     * @param maxRating Maximum rating value.
     * @return A list of restaurants within the specified rating range.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.rating BETWEEN :minRating AND :maxRating")
    List<Restaurant> findByRatingRange(@Param("minRating") double minRating, @Param("maxRating") double maxRating);

    /**
     * Finds restaurants by name and full address.
     *
     * @param name       The name of the restaurant.
     * @param street     The street of the address.
     * @param city       The city of the address.
     * @param state      The state of the address.
     * @param postalCode The postal code of the address.
     * @return A list of restaurants matching the given name and full address.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.name = :name AND r.address.street = :street AND r.address.city = :city AND r.address.state = :state AND r.address.postalCode = :postalCode")
    List<Restaurant> findByNameAndFullAddress(
            @Param("name") String name,
            @Param("street") String street,
            @Param("city") String city,
            @Param("state") String state,
            @Param("postalCode") String postalCode
    );

    /**
     * Checks if a restaurant exists by its name and address ID.
     *
     * @param name     The name of the restaurant.
     * @param addressId The ID of the associated address.
     * @return True if a restaurant exists with the given name and address, false otherwise.
     */
    boolean existsByNameAndAddress_Id(String name, long addressId);

    /**
     * Checks if a restaurant exists by its full address.
     *
     * @param name      The name of the restaurant.
     * @param street    The street of the address.
     * @param city      The city of the address.
     * @param state     The state of the address.
     * @param postalCode The postal code of the address.
     * @return True if a restaurant exists with the given name and full address, false otherwise.
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM Restaurant r WHERE r.name = :name AND r.address.street = :street AND r.address.city = :city AND r.address.state = :state AND r.address.postalCode = :postalCode")
    boolean existsByNameAndFullAddress(@Param("name") String name, @Param("street") String street, @Param("city") String city, @Param("state") String state, @Param("postalCode") String postalCode);

    /**
     * Checks if a restaurant exists by its name and city.
     *
     * @param name The name of the restaurant.
     * @param city The city of the restaurant.
     * @return True if a restaurant exists with the given name and city, false otherwise.
     */
    boolean existsByNameAndAddress_City(String name, String city);

    /**
     * Checks if a restaurant exists by its name and state.
     *
     * @param name  The name of the restaurant.
     * @param state The state of the restaurant.
     * @return True if a restaurant exists with the given name and state, false otherwise.
     */
    boolean existsByNameAndAddress_State(String name, String state);
}
