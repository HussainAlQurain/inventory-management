package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.service.LocationService;
import org.springframework.stereotype.Component;

@Component
public class LocationIdMapper {
    private final LocationService locationService;

    public LocationIdMapper(LocationService locationService) {
        this.locationService = locationService;
    }

    public Location map(Long locationId) {
        if (locationId == null) return null;
        return locationService.findOne(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    public Long map(Location location) {
        return (location != null) ? location.getId() : null;
    }
}
