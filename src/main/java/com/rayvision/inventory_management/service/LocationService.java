package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Location;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    Location save(Long companyId, Location location);
    List<Location> findAll();
    Optional<Location> findOne(Long id);
    Location partialUpdate(Long id, Location location);
    boolean isExists(Long id);
    List<Location> findByCompanyId(Long companyId);
    List<Location> findByUserId(Long userId);
    List<Location> findByCompanyIdAndUserId(Long companyId, Long userId);
    void delete(Long id);
}
