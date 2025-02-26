package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.mappers.StorageAreaMapper;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.StorageArea;
import com.rayvision.inventory_management.model.dto.StorageAreaDTO;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.StorageAreaRepository;
import com.rayvision.inventory_management.service.StorageAreaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class StorageAreaServiceImpl implements StorageAreaService {

    private final StorageAreaRepository storageAreaRepository;
    private final LocationRepository locationRepository;
    private final StorageAreaMapper storageAreaMapper;

    public StorageAreaServiceImpl(StorageAreaRepository storageAreaRepository,
                                  LocationRepository locationRepository,
                                  StorageAreaMapper storageAreaMapper) {
        this.storageAreaRepository = storageAreaRepository;
        this.locationRepository = locationRepository;
        this.storageAreaMapper = storageAreaMapper;
    }

    @Override
    public StorageArea create(Long locationId, StorageAreaDTO dto) {
        // 1) Verify location
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        // 2) Convert DTO -> Entity
        //    The mapper will set location.id = dto.getLocationId(),
        //    but we actually want the location to come from DB.
        StorageArea entity = storageAreaMapper.toEntity(dto);
        entity.setId(null);  // ensure a new record
        entity.setLocation(loc); // override with the real location entity

        return storageAreaRepository.save(entity);
    }

    @Override
    public StorageArea update(Long locationId, Long storageAreaId, StorageAreaDTO dto) {
        // 1) Find existing
        StorageArea existing = storageAreaRepository.findById(storageAreaId)
                .orElseThrow(() -> new RuntimeException("StorageArea not found: " + storageAreaId));

        // 2) Ensure it belongs to the same location
        if (!existing.getLocation().getId().equals(locationId)) {
            throw new RuntimeException("StorageArea does not belong to location " + locationId);
        }

        // 3) Update fields (full or partial logic)
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        // Typically do not allow changing location for an existing storage area

        return storageAreaRepository.save(existing);
    }

    @Override
    public void delete(Long locationId, Long storageAreaId) {
        StorageArea existing = storageAreaRepository.findById(storageAreaId)
                .orElseThrow(() -> new RuntimeException("StorageArea not found: " + storageAreaId));
        if (!existing.getLocation().getId().equals(locationId)) {
            throw new RuntimeException("StorageArea does not belong to location " + locationId);
        }
        storageAreaRepository.delete(existing);
    }

    @Override
    public StorageArea getOne(Long locationId, Long storageAreaId) {
        StorageArea existing = storageAreaRepository.findById(storageAreaId)
                .orElseThrow(() -> new RuntimeException("StorageArea not found: " + storageAreaId));
        if (!existing.getLocation().getId().equals(locationId)) {
            throw new RuntimeException("StorageArea does not belong to location " + locationId);
        }
        return existing;
    }

    @Override
    public List<StorageArea> getAllByLocation(Long locationId) {
        // With CrudRepository, we don’t have a direct “findByLocation” method by default.
        // We can either define a custom query or filter in memory:
        return StreamSupport.stream(storageAreaRepository.findAll().spliterator(), false)
                .filter(sa -> sa.getLocation() != null && sa.getLocation().getId().equals(locationId))
                .collect(Collectors.toList());
    }

}
