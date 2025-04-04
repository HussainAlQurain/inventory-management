package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.LocationMapper;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.LocationDTO;
import com.rayvision.inventory_management.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations")
public class LocationController {
    private final LocationService locationService;
    private final LocationMapper locationMapper;

    public LocationController(LocationService locationService, LocationMapper locationMapper) {
        this.locationService = locationService;
        this.locationMapper = locationMapper;
    }

    @PostMapping("/{companyId}")
    public ResponseEntity<LocationDTO> createLocation(@PathVariable Long companyId,
                                                      @RequestBody LocationDTO locationDTO) {
        // Convert DTO to entity
        Location location = locationMapper.toEntity(locationDTO);
        Location savedLocation = this.locationService.save(companyId, location);
        // Convert saved entity back to DTO
        LocationDTO responseDTO = locationMapper.toDTO(savedLocation);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }


    @PostMapping("/{locationId}/users")
    public ResponseEntity<List<Users>> createUserLocation(@PathVariable Long locationId, @RequestBody List<Long> userIds) {
        List<Users> savedUsers = locationService.addUsersToLocation(locationId, userIds);
        return new ResponseEntity<>(savedUsers, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        List<Location> locations = this.locationService.findAll();
        List<LocationDTO> dtos = locations.stream()
                .map(locationMapper::toDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }


    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<LocationDTO>> getLocationsByCompanyId(@PathVariable Long companyId) {
        List<Location> locations = this.locationService.findByCompanyId(companyId);
        List<LocationDTO> dtos = locations.stream()
                .map(locationMapper::toDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }


    @GetMapping("/{locationId}")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable Long locationId) {
        Optional<Location> foundLocation = locationService.findOne(locationId);
        if (foundLocation.isPresent()) {
            return ResponseEntity.ok(locationMapper.toDTO(foundLocation.get()));
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{locationId}/users")
    public ResponseEntity<List<?>> getUserLocationsByLocationId(@PathVariable Long locationId) {
        return ResponseEntity.ok(locationService.findUsersByLocationId(locationId));
    }

    @DeleteMapping("/{locationId}/users/{userId}")
    public ResponseEntity<Void> deleteUserLocation(@PathVariable Long locationId, @PathVariable Long userId) {
        locationService.removeUserFromLocation(locationId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/company/{companyId}/user/{userId}")
    public ResponseEntity<List<LocationDTO>> getLocationsForUserInCompany(
            @PathVariable Long companyId,
            @PathVariable Long userId
    ) {
        // 1) Fetch matching locations from the service
        List<Location> locations =
                locationService.findByCompanyIdAndUserId(companyId, userId);

        // 2) Convert to DTO
        List<LocationDTO> dtos = locations.stream()
                .map(locationMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }


}
