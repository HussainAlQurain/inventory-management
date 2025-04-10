package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.LocationUser;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.LocationUserRepository;
import com.rayvision.inventory_management.repository.UserRepository;
import com.rayvision.inventory_management.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    private final CompanyRepository companyRepository;

    private final LocationUserRepository locationUserRepository;

    private final UserRepository userRepository;

    public LocationServiceImpl(LocationRepository locationRepository, CompanyRepository companyRepository, LocationUserRepository locationUserRepository, UserRepository userRepository) {
        this.locationRepository = locationRepository;
        this.companyRepository = companyRepository;
        this.locationUserRepository = locationUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public Location save(Long companyId, Location location) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));
        location.setCompany(company);
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
//            Optional.ofNullable(location.getCompany()).ifPresent(existingLocation::setCompany); // don't allow reassigning a location to different company

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
        return null;
    }

    @Override
    public List<Location> findByCompanyIdAndUserId(Long companyId, Long userId) {
        return locationRepository.findByCompanyIdAndUserId(companyId, userId);
    }

    @Override
    public void delete(Long id) {
        if (locationRepository.existsById(id)) {
            locationRepository.deleteById(id);
        } else {
            throw new RuntimeException("Location doesn't exist");
        }
    }

    @Transactional
    @Override
    public List<Users> addUsersToLocation(Long locationId, List<Long> userIds) {

        // Fetch location
        Location location = locationRepository.findById(locationId).orElseThrow(() -> new RuntimeException("Location not found"));

        // Fetch users by Ids
        List<Users> users = userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            throw new RuntimeException("No valid users found for the given IDs.");
        }
        // Check for valid users
        Set<Long> userIdSet = users.stream().map(Users::getId).collect(Collectors.toSet());
        List<Long> invalidUserIds = userIds.stream().filter(userId -> !userIdSet.contains(userId)).collect(Collectors.toList());
        if(!invalidUserIds.isEmpty()) {
            throw new RuntimeException("Invalid users found for the given IDs: " + invalidUserIds);
        }

        //Filter out Users already associated with the location
        Set<Long> existingUserIds = location.getLocationUsers().stream().map(locationUser -> locationUser.getUser().getId()).collect(Collectors.toSet());
        List<LocationUser> newLocationUsers = users.stream().filter(user -> !existingUserIds.contains(user.getId())).map(user -> LocationUser.builder().user(user).location(location).build()).collect(Collectors.toList());
        // Save only new LocationUser associations
        if(!newLocationUsers.isEmpty()) {
            locationUserRepository.saveAll(newLocationUsers);
        }
        return newLocationUsers.stream().map(LocationUser::getUser).toList();
    }

    @Override
    public List<Users> findUsersByLocationId(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + locationId));

        if (location.getLocationUsers().isEmpty()) {
            throw new RuntimeException("No users found for the given location: " + location.getName());
        }
        return location.getLocationUsers().stream()
                .map(LocationUser::getUser)
                .collect(Collectors.toList());
    }

    @Override
    public void removeUserFromLocation(Long locationId, Long userId) {
        Optional<LocationUser> locationUser = locationUserRepository.findByLocationIdAndUserId(locationId, userId);
        if (locationUser.isPresent()) {
            locationUserRepository.delete(locationUser.get());
        }
        else {
            throw new RuntimeException("User doesn't exist");
        }
    }

    @Override
    public Optional<Location> findByIdAndCompanyId(Long locationId, Long companyId) {
        return locationRepository.findByIdAndCompanyId(locationId, companyId);
    }
}
