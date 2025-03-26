package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.SubRecipeType;
import com.rayvision.inventory_management.mappers.PrepItemLocationMapper;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.PrepItemLocation;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.PrepItemLocationDTO;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.PrepItemLocationRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.service.PrepItemLocationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrepItemLocationServiceImpl implements PrepItemLocationService {

    private final PrepItemLocationRepository prepItemLocationRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final LocationRepository locationRepository;
    private final PrepItemLocationMapper prepItemLocationMapper;

    public PrepItemLocationServiceImpl(PrepItemLocationRepository repo,
                                       SubRecipeRepository subRecipeRepo,
                                       LocationRepository locRepo,
                                       PrepItemLocationMapper prepItemLocationMapper) {
        this.prepItemLocationRepository = repo;
        this.subRecipeRepository = subRecipeRepo;
        this.locationRepository = locRepo;
        this.prepItemLocationMapper = prepItemLocationMapper;
    }

    @Override
    public PrepItemLocation create(PrepItemLocationDTO dto) {
        // 1) Convert DTO -> Entity (initially)
        PrepItemLocation entity = prepItemLocationMapper.toEntity(dto);

        // 2) But we must verify subRecipeId + locationId
        Long subRecipeId = dto.getSubRecipeId();
        SubRecipe sr = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));

        Long locationId = dto.getLocationId();
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        // 3) Link the references
        entity.setSubRecipe(sr);
        entity.setLocation(loc);

        // 4) Save
        return prepItemLocationRepository.save(entity);
    }

    @Override
    public PrepItemLocation update(Long bridgingId, PrepItemLocationDTO dto) {
        PrepItemLocation existing = prepItemLocationRepository.findById(bridgingId)
                .orElseThrow(() -> new RuntimeException("PrepItemLocation not found: " + bridgingId));

        // Partial or full update
        if (dto.getOnHand() != null) {
            existing.setOnHand(dto.getOnHand());
        }
        if (dto.getMinOnHand() != null) {
            existing.setMinOnHand(dto.getMinOnHand());
        }
        if (dto.getPar() != null) {
            existing.setPar(dto.getPar());
        }
        if (dto.getLastCount() != null) {
            existing.setLastCount(dto.getLastCount());
        }
        if (dto.getLastCountDate() != null) {
            // if you add lastCountDate to entity
            // existing.setLastCountDate(dto.getLastCountDate());
        }

        return prepItemLocationRepository.save(existing);
    }

    @Override
    public PrepItemLocation createOrUpdate(PrepItemLocationDTO dto) {
        // If you want logic like find existing by subRecipeId + locationId
        // to either update or create new
        Long srId = dto.getSubRecipeId();
        Long locId = dto.getLocationId();
        Optional<PrepItemLocation> existingOpt = prepItemLocationRepository
                .findBySubRecipeIdAndLocationId(dto.getSubRecipeId(), dto.getLocationId());
        if (existingOpt.isPresent()) {
            // update relevant fields
            PrepItemLocation existing = existingOpt.get();
            if (dto.getOnHand() != null) existing.setOnHand(dto.getOnHand());
            if (dto.getMinOnHand() != null) existing.setMinOnHand(dto.getMinOnHand());
            if (dto.getPar() != null) existing.setPar(dto.getPar());
            if (dto.getLastCount() != null) existing.setLastCount(dto.getLastCount());
            if (dto.getLastCountDate() != null) existing.setLastCountDate(dto.getLastCountDate());
            return prepItemLocationRepository.save(existing);
        } else {
            // create new
            return create(dto);
        }
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

    @Override
    public List<PrepItemLocation> getBySubRecipe(Long subRecipeId) {
        return prepItemLocationRepository.findBySubRecipeId(subRecipeId);
    }

    @Override
    public List<PrepItemLocation> getByLocation(Long locationId) {
        return prepItemLocationRepository.findByLocationId(locationId);
    }

    @Override
    public void setThresholdsForLocation(Long subRecipeId, Long locationId, Double minOnHand, Double parLevel) {
        // Validate subrecipe type
        SubRecipe subRecipe = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found"));
        if (subRecipe.getType() != SubRecipeType.PREPARATION) {
            throw new IllegalArgumentException("SubRecipe must be of type PREPARATION");
        }

        // Validate values
        if (minOnHand != null && minOnHand < 0) {
            throw new IllegalArgumentException("minOnHand cannot be negative");
        }
        if (parLevel != null && parLevel < 0) {
            throw new IllegalArgumentException("parLevel cannot be negative");
        }

        PrepItemLocation prepLocation = prepItemLocationRepository
                .findBySubRecipeIdAndLocationId(subRecipeId, locationId)
                .orElseThrow(() -> new RuntimeException("PrepItemLocation not found"));

        if (minOnHand != null) prepLocation.setMinOnHand(minOnHand);
        if (parLevel != null) prepLocation.setPar(parLevel);

        prepItemLocationRepository.save(prepLocation);
    }

    @Override
    public void bulkSetThresholdsForCompany(Long companyId, Long subRecipeId, Double minOnHand, Double parLevel) {
        // Validate subrecipe type
        SubRecipe subRecipe = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found"));
        if (subRecipe.getType() != SubRecipeType.PREPARATION) {
            throw new IllegalArgumentException("SubRecipe must be of type PREPARATION");
        }

        List<Location> companyLocations = locationRepository.findByCompanyId(companyId);

        companyLocations.forEach(location -> {
            PrepItemLocationDTO dto = PrepItemLocationDTO.builder()
                    .subRecipeId(subRecipeId)
                    .locationId(location.getId())
                    .minOnHand(minOnHand)
                    .par(parLevel)
                    .build();

            createOrUpdate(dto);
        });
    }

}
