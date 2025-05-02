package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    Location save(Long companyId, Location location);
    List<Location> findAll();
    Optional<Location> findOne(Long id);
    Location partialUpdate(Long id, Location location);
    boolean isExists(Long id);
    List<Location> findByCompanyId(Long companyId);
    
    /**
     * Find locations by company ID with pagination and optional search
     * @param companyId Company ID
     * @param search Optional search term for location name
     * @param pageable Pagination information
     * @return Page of locations
     */
    Page<Location> findByCompanyIdPaginated(Long companyId, String search, Pageable pageable);

    List<Location> findByUserId(Long userId);
    List<Location> findByCompanyIdAndUserId(Long companyId, Long userId);
    void delete(Long id);
    List<Users> findUsersByLocationId(Long locationId);
    List<Users> addUsersToLocation(Long locationId, List<Long> userIds);
    void removeUserFromLocation(Long locationId, Long userId);
    Optional<Location> findByIdAndCompanyId(Long locationId, Long companyId);
}
