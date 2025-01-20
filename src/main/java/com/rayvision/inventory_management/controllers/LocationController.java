package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/locations")
public class LocationController {
    private LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/{companyId}")
    public ResponseEntity<Location> createLocation(@PathVariable Long companyId, @RequestBody Location location) {
        Location savedLocation = this.locationService.save(companyId, location);
        return new ResponseEntity<>(savedLocation, HttpStatus.CREATED);
    }

    @PostMapping("/{locationId}/users")
    public ResponseEntity<List<Users>> createUserLocation(@PathVariable Long locationId, @RequestBody List<Long> userIds) {
        List<Users> savedUsers = locationService.addUsersToLocation(locationId, userIds);
        return new ResponseEntity<>(savedUsers, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = this.locationService.findAll();
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<Location>> getLocationsByCompanyId(@PathVariable Long companyId) {
        List<Location> locations = this.locationService.findByCompanyId(companyId);
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<Location> getLocationById(@PathVariable Long locationId) {
        Optional<Location> foundLocation = locationService.findOne(locationId);
        return foundLocation.map(location -> {
            return new ResponseEntity<>(location, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
