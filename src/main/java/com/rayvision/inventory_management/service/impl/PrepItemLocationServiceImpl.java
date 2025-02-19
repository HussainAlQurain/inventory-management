package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.PrepItemLocation;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.PrepItemLocationRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.service.PrepItemLocationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PrepItemLocationServiceImpl implements PrepItemLocationService {

    private final PrepItemLocationRepository prepItemLocationRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final LocationRepository locationRepository;

    public PrepItemLocationServiceImpl(PrepItemLocationRepository prepItemLocationRepository,
                                       SubRecipeRepository subRecipeRepository,
                                       LocationRepository locationRepository) {
        this.prepItemLocationRepository = prepItemLocationRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<PrepItemLocation> getBySubRecipe(Long subRecipeId) {
        return prepItemLocationRepository.findBySubRecipeId(subRecipeId);
    }

    @Override
    public List<PrepItemLocation> getByLocation(Long locationId) {
        return prepItemLocationRepository.findByLocationId(locationId);
    }

    @Override
    public PrepItemLocation create(Long subRecipeId, Long locationId, PrepItemLocation prepLoc) {
        SubRecipe sr = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        prepLoc.setSubRecipe(sr);
        prepLoc.setLocation(loc);
        return prepItemLocationRepository.save(prepLoc);
    }

    @Override
    public PrepItemLocation update(Long bridgingId, PrepItemLocation patch) {
        PrepItemLocation existing = prepItemLocationRepository.findById(bridgingId)
                .orElseThrow(() -> new RuntimeException("PrepItemLocation not found: " + bridgingId));

        // fully or partially update
        existing.setOnHand(patch.getOnHand());
        existing.setMinOnHand(patch.getMinOnHand());
        existing.setPar(patch.getPar());
        existing.setLastCount(patch.getLastCount());

        // optionally prevent changing subRecipe / location
        return prepItemLocationRepository.save(existing);
    }

    @Override
    public void delete(Long bridgingId) {
        PrepItemLocation existing = prepItemLocationRepository.findById(bridgingId)
                .orElseThrow(() -> new RuntimeException("PrepItemLocation not found: " + bridgingId));
        prepItemLocationRepository.delete(existing);
    }

    @Override
    public Optional<PrepItemLocation> getOne(Long bridgingId) {
        return prepItemLocationRepository.findById(bridgingId);
    }

}
