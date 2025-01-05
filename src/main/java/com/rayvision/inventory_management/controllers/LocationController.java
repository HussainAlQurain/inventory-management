package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {
    private LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(@RequestBody Location location) {
        Location savedLocation = this.locationService.save(location);
        return new ResponseEntity<>(savedLocation, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = this.locationService.findAll();
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }
}
