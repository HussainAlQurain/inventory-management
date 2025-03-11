package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.mappers.InventoryItemLocationMapper;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;
import com.rayvision.inventory_management.repository.InventoryItemLocationRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryItemLocationServiceImpl implements InventoryItemLocationService {
    private final InventoryItemLocationRepository repository;
    private final InventoryItemRepository itemRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemLocationMapper mapper;

    public InventoryItemLocationServiceImpl(InventoryItemLocationRepository repository,
                                            InventoryItemRepository itemRepository,
                                            LocationRepository locationRepository,
                                            InventoryItemLocationMapper mapper) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.mapper = mapper;
    }

    @Override
    public InventoryItemLocation create(InventoryItemLocationDTO dto) {
        // We convert DTO -> Entity
        // But we must fetch the references ourselves to confirm existence
        InventoryItem item = itemRepository.findById(dto.getInventoryItemId())
                .orElseThrow(() -> new RuntimeException("InventoryItem not found: " + dto.getInventoryItemId()));
        Location loc = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found: " + dto.getLocationId()));

        InventoryItemLocation entity = new InventoryItemLocation();
        entity.setInventoryItem(item);
        entity.setLocation(loc);
        entity.setMinOnHand(dto.getMinOnHand());
        entity.setParLevel(dto.getParLevel());
        entity.setOnHand(dto.getOnHand());
        entity.setLastCount(dto.getLastCount());
        entity.setLastCountDate(dto.getLastCountDate());

        return repository.save(entity);
    }

    @Override
    public InventoryItemLocation update(Long bridgingId, InventoryItemLocationDTO dto) {
        InventoryItemLocation existing = repository.findById(bridgingId)
                .orElseThrow(() -> new RuntimeException("Not found bridging with id=" + bridgingId));

        // We do partial or full update
        Optional.ofNullable(dto.getMinOnHand()).ifPresent(existing::setMinOnHand);
        Optional.ofNullable(dto.getParLevel()).ifPresent(existing::setParLevel);
        Optional.ofNullable(dto.getOnHand()).ifPresent(existing::setOnHand);
        Optional.ofNullable(dto.getLastCount()).ifPresent(existing::setLastCount);
        Optional.ofNullable(dto.getLastCountDate()).ifPresent(existing::setLastCountDate);

        return repository.save(existing);
    }

    @Override
    public void delete(Long bridgingId) {
        InventoryItemLocation existing = repository.findById(bridgingId)
                .orElseThrow(() -> new RuntimeException("Not found bridging with id=" + bridgingId));
        repository.delete(existing);
    }

    @Override
    public Optional<InventoryItemLocation> getOne(Long bridgingId) {
        return repository.findById(bridgingId);
    }

    @Override
    public InventoryItemLocation createOrUpdateByItemAndLocation(InventoryItemLocationDTO dto) {
        // see if bridging already exists
        Optional<InventoryItemLocation> existingOpt =
                repository.findByInventoryItemIdAndLocationId(dto.getInventoryItemId(), dto.getLocationId());

        if (existingOpt.isPresent()) {
            InventoryItemLocation existing = existingOpt.get();
            // update relevant fields
            if (dto.getMinOnHand() != null) existing.setMinOnHand(dto.getMinOnHand());
            if (dto.getParLevel() != null) existing.setParLevel(dto.getParLevel());
            if (dto.getOnHand() != null) existing.setOnHand(dto.getOnHand());
            if (dto.getLastCount() != null) existing.setLastCount(dto.getLastCount());
            if (dto.getLastCountDate() != null) existing.setLastCountDate(dto.getLastCountDate());
            return repository.save(existing);
        } else {
            // create new
            return create(dto);
        }
    }

    @Override
    public List<InventoryItemLocation> getByItem(Long itemId) {
        return repository.findByInventoryItemId(itemId);
    }

    @Override
    public List<InventoryItemLocation> getByLocation(Long locationId) {
        return repository.findByLocationId(locationId);
    }


}
