package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    public LocationServiceImpl(@Autowired LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public Location save(Location location) {
        return locationRepository.save(location);
    }

    @Override
    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    @Override
    public Optional<Location> findOne(Long id) {
        return locationRepository.findById(id);
    }

    @Override
    public Location partialUpdate(Long id, Location location) {
        return locationRepository.findById(id).map(existingLocation -> {
            Optional.ofNullable(location.getName()).ifPresent(existingLocation::setName);
            Optional.ofNullable(location.getCode()).ifPresent(existingLocation::setCode);
            Optional.ofNullable(location.getAddress()).ifPresent(existingLocation::setAddress);
            Optional.ofNullable(location.getCity()).ifPresent(existingLocation::setCity);
            Optional.ofNullable(location.getState()).ifPresent(existingLocation::setState);
            Optional.ofNullable(location.getZip()).ifPresent(existingLocation::setZip);
            Optional.ofNullable(location.getPhone()).ifPresent(existingLocation::setPhone);
            Optional.ofNullable(location.getCompany()).ifPresent(existingLocation::setCompany);

//            // Update suppliers (add/remove)
//            if (location.getAuthorizedSuppliers() != null) {
//                // Add or remove suppliers
//                existingLocation.getAuthorizedSuppliers().clear(); // Clear current ones
//                existingLocation.getAuthorizedSuppliers().addAll(location.getAuthorizedSuppliers()); // Add new ones
//            }
//
//            // Update users (add/remove)
//            if (location.getUsers() != null) {
//                existingLocation.getUsers().clear(); // Clear current ones
//                existingLocation.getUsers().addAll(location.getUsers()); // Add new ones
//            }
//
//            // Update assortments (add/remove)
//            if (location.getAssortments() != null) {
//                existingLocation.getAssortments().clear(); // Clear current ones
//                existingLocation.getAssortments().addAll(location.getAssortments()); // Add new ones
//            }
//
            return locationRepository.save(existingLocation);
        }).orElseThrow(() -> new RuntimeException("Location doesn't exist"));
    }

    @Override
    public boolean isExists(Long id) {
        return locationRepository.existsById(id);
    }

    @Override
    public List<Location> findByCompanyId(Long companyId) {
        return locationRepository.findAll().stream()
                .filter(location -> location.getCompany().getId().equals(companyId))
                .toList();
    }

    @Override
    public List<Location> findByUserId(Long userId) {
        return locationRepository.findAll().stream()
                .filter(location -> location.getUsers().stream()
                        .anyMatch(user -> user.getId().equals(userId)))
                .toList();
    }

    @Override
    public List<Location> findByCompanyIdAndUserId(Long companyId, Long userId) {
        return locationRepository.findAll().stream()
                .filter(location -> location.getCompany().getId().equals(companyId) &&
                        location.getUsers().stream()
                                .anyMatch(user -> user.getId().equals(userId)))
                .toList();
    }

    @Override
    public void delete(Long id) {
        if (locationRepository.existsById(id)) {
            locationRepository.deleteById(id);
        } else {
            throw new RuntimeException("Location doesn't exist");
        }
    }
}
