package net.guilhermegomes.backend.repository;

import net.guilhermegomes.backend.domain.address.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Address entities.
 * Provides CRUD operations and custom queries for searching addresses.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Finds an address by its postal code.
     *
     * @param postalCode The postal code to search for.
     * @return An optional containing the found address, if any.
     */
    Optional<Address> findByPostalCode(String postalCode);

    /**
     * Finds all addresses in a specific city.
     *
     * @param city The city name to search for.
     * @return A list of addresses in the specified city.
     */
    List<Address> findByCity(String city);

    /**
     * Finds all addresses in a specific state.
     *
     * @param state The state code to search for.
     * @return A list of addresses in the specified state.
     */
    List<Address> findByState(String state);

    /**
     * Finds all addresses within a given country.
     *
     * @param country The country name to search for.
     * @return A list of addresses in the specified country.
     */
    List<Address> findByCountry(String country);

    /**
     * Custom query to find addresses by street name and city.
     *
     * @param street The street name.
     * @param city   The city name.
     * @return A list of addresses matching the criteria.
     */
    @Query("SELECT a FROM Address a WHERE a.street LIKE %:street% AND a.city = :city")
    List<Address> findByStreetAndCity(@Param("street") String street, @Param("city") String city);

    /**
     * Finds all addresses within a given latitude and longitude range.
     * Useful for location-based searches.
     *
     * @param minLat Minimum latitude.
     * @param maxLat Maximum latitude.
     * @param minLon Minimum longitude.
     * @param maxLon Maximum longitude.
     * @return A list of addresses within the specified geographic bounds.
     */
    @Query("SELECT a FROM Address a WHERE a.latitude BETWEEN :minLat AND :maxLat AND a.longitude BETWEEN :minLon AND :maxLon")
    List<Address> findByGeographicBounds(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                         @Param("minLon") double minLon, @Param("maxLon") double maxLon);

    /**
     * Checks if an address exists by its street, city, and postal code.
     *
     * @param street     The street name.
     * @param city       The city name.
     * @param postalCode The postal code.
     * @return True if an address exists with the given details, false otherwise.
     */
    boolean existsByStreetAndCityAndPostalCode(String street, String city, String postalCode);

    /**
     * Finds the address of a restaurant by street, city, and postal code.
     *
     * @param street     The street name.
     * @param city       The city name.
     * @param postalCode The postal code.
     * @return An optional containing the restaurant address, if found.
     */
    @Query("SELECT a FROM Address a WHERE a.street = :street AND a.city = :city AND a.postalCode = :postalCode")
    Optional<Address> findAddressByStreetCityAndPostalCode(@Param("street") String street,
                                                              @Param("city") String city,
                                                              @Param("postalCode") String postalCode);
}