package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.mappers.InventoryItemLocationMapper;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;
import com.rayvision.inventory_management.model.dto.ItemOnHandTotalsDTO;
import com.rayvision.inventory_management.model.records.LocationInventoryDTO;
import com.rayvision.inventory_management.repository.InventoryItemLocationRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryItemLocationServiceImpl implements InventoryItemLocationService {
    private final InventoryItemLocationRepository repository;
    private final InventoryItemRepository itemRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemLocationMapper inventoryItemLocationMapper;

    public InventoryItemLocationServiceImpl(InventoryItemLocationRepository repository,
                                            InventoryItemRepository itemRepository,
                                            LocationRepository locationRepository,
                                            InventoryItemLocationMapper inventoryItemLocationMapper) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemLocationMapper = inventoryItemLocationMapper;
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
        // Validate minOnHand (if provided)
        if (dto.getMinOnHand() != null && dto.getMinOnHand() < 0) {
            throw new IllegalArgumentException("minOnHand cannot be negative");
        }

        // Validate parLevel (if provided)
        if (dto.getParLevel() != null && dto.getParLevel() < 0) {
            throw new IllegalArgumentException("parLevel cannot be negative");
        }
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

    @Override
    public void bulkUpdate(Long companyId, Long itemId, Double newMin, Double newPar) {
        // find bridging rows
        // typically we do something like:
        List<InventoryItemLocation> bridgingList = repository.findByInventoryItemId(itemId);
        // if you want to check company as well, ensure bridgingList only from that company's locations
        bridgingList.forEach(b -> {
            if (b.getLocation().getCompany().getId().equals(companyId)) {
                if (newMin != null) b.setMinOnHand(newMin);
                if (newPar != null) b.setParLevel(newPar);
            }
        });
        repository.saveAll(bridgingList);
    }

    @Override
    public List<InventoryItemLocation> findByItemId(Long itemId) {
        return repository.findByInventoryItemId(itemId);
    }


    @Override
    public void setThresholdsForLocation(Long itemId, Long locationId, Double minOnHand, Double parLevel) {
        // Validate minOnHand (if provided)
        if (minOnHand != null && minOnHand < 0) {
            throw new IllegalArgumentException("minOnHand cannot be negative");
        }

        // Validate parLevel (if provided)
        if (parLevel != null && parLevel < 0) {
            throw new IllegalArgumentException("parLevel cannot be negative");
        }

        InventoryItemLocation itemLocation = repository.findByInventoryItemIdAndLocationId(itemId, locationId)
                .orElseThrow(() -> new RuntimeException("Item-location relationship not found"));

        if (minOnHand != null) itemLocation.setMinOnHand(minOnHand);
        if (parLevel != null) itemLocation.setParLevel(parLevel);

        repository.save(itemLocation);
    }

    @Override
    public void bulkSetThresholdsForCompany(Long companyId, Long itemId, Double minOnHand, Double parLevel) {
        // Get all locations in the company
        List<Location> companyLocations = locationRepository.findByCompanyId(companyId);

        companyLocations.forEach(location -> {
            // Use existing createOrUpdate logic to handle missing entries
            InventoryItemLocationDTO dto = InventoryItemLocationDTO.builder()
                    .inventoryItemId(itemId)
                    .locationId(location.getId())
                    .minOnHand(minOnHand)
                    .parLevel(parLevel)
                    .build();

            createOrUpdateByItemAndLocation(dto);
        });
    }

    @Override
    public void patchThresholds(Long itemId, Long locationId, Double minOnHand, Double parLevel) {
        // Validate minOnHand (if provided)
        if (minOnHand != null && minOnHand < 0) {
            throw new IllegalArgumentException("minOnHand cannot be negative");
        }

        // Validate parLevel (if provided)
        if (parLevel != null && parLevel < 0) {
            throw new IllegalArgumentException("parLevel cannot be negative");
        }

        InventoryItemLocation itemLocation = repository.findByInventoryItemIdAndLocationId(itemId, locationId)
                .orElseThrow(() -> new RuntimeException("Item-location relationship not found"));

        if (minOnHand != null) itemLocation.setMinOnHand(minOnHand);
        if (parLevel != null) itemLocation.setParLevel(parLevel);

        repository.save(itemLocation);
    }

    @Override
    public Optional<InventoryItemLocation> findByInventoryItemIdAndLocationId(Long itemId, Long locationId) {
        return repository.findByInventoryItemIdAndLocationId(itemId, locationId);
    }

    /**
     * Add `deltaQty` to the bridging's onHand.
     * If bridging does not exist, we create it (with onHand=deltaQty).
     */
    @Override
    public void incrementOnHand(Long itemId, Long locationId, double deltaQty) {
        if (Math.abs(deltaQty) < 0.000001) return;

        Optional<InventoryItemLocation> opt =
                repository.findByInventoryItemIdAndLocationId(itemId, locationId);

        if (opt.isPresent()) {
            InventoryItemLocation bridging = opt.get();
            double oldVal = (bridging.getOnHand() != null) ? bridging.getOnHand() : 0.0;
            bridging.setOnHand(oldVal + deltaQty);
            repository.save(bridging);
        } else {
            // create new bridging
            InventoryItem item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found " + itemId));
            Location loc = locationRepository.findById(locationId)
                    .orElseThrow(() -> new RuntimeException("Location not found " + locationId));

            InventoryItemLocation newBridging = new InventoryItemLocation();
            newBridging.setInventoryItem(item);
            newBridging.setLocation(loc);
            newBridging.setOnHand(deltaQty);
            repository.save(newBridging);
        }
    }

    /**
     * Efficiently calculate the total on-hand quantity and value for an inventory item
     * across all locations in a company.
     * 
     * @param itemId The inventory item ID
     * @param companyId The company ID to filter locations
     * @return A DTO containing the total quantity and value
     */
    @Override
    public ItemOnHandTotalsDTO getItemOnHandTotals(Long itemId, Long companyId) {
        // Get the inventory item to access its current price
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        
        // Find all inventory item locations for this item
        List<InventoryItemLocation> locations = repository.findByInventoryItemId(itemId);
        
        // Filter by company and calculate totals in a single pass
        double totalQuantity = 0.0;
        double currentPrice = item.getCurrentPrice() != null ? item.getCurrentPrice() : 0.0;
        
        for (InventoryItemLocation loc : locations) {
            // Only include locations that belong to the specified company
            if (loc.getLocation().getCompany().getId().equals(companyId)) {
                // Add to the total quantity if onHand is not null
                if (loc.getOnHand() != null) {
                    totalQuantity += loc.getOnHand();
                }
            }
        }
        
        // Calculate total value
        double totalValue = totalQuantity * currentPrice;
        
        // Return the totals in a DTO
        return ItemOnHandTotalsDTO.builder()
                .itemId(itemId)
                .totalQuantity(totalQuantity)
                .totalValue(totalValue)
                .build();
    }

    /**
     * Get paginated and searchable inventory item locations for an item
     * 
     * @param itemId The inventory item ID
     * @param companyId The company ID
     * @param locationSearch Optional search term for location names
     * @param pageable Pagination and sorting parameters
     * @return A Page of LocationInventoryDTO
     */
    @Override
    public Page<LocationInventoryDTO> getPaginatedLocationsForItem(
            Long itemId,
            Long companyId,
            String locationSearch,
            Pageable pageable
    ) {
        // Get the inventory item to access its current price
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        
        // Get the current price
        double currentPrice = item.getCurrentPrice() != null ? item.getCurrentPrice() : 0.0;
        
        // Get all inventory item locations for this item
        List<InventoryItemLocation> allLocations = repository.findByInventoryItemId(itemId);
        
        // Filter locations by company and optionally by search term
        List<InventoryItemLocation> filteredLocations = allLocations.stream()
                .filter(loc -> loc.getLocation().getCompany().getId().equals(companyId))
                .filter(loc -> {
                    if (!StringUtils.hasText(locationSearch)) {
                        return true; // No search filter
                    }
                    // Search in location name (case-insensitive)
                    return loc.getLocation().getName().toLowerCase().contains(locationSearch.toLowerCase());
                })
                .collect(Collectors.toList());
        
        // Calculate total elements for pagination info
        int totalElements = filteredLocations.size();
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredLocations.size());
        
        // If start is beyond list size, return empty page
        List<InventoryItemLocation> pagedLocations = start < end 
                ? filteredLocations.subList(start, end) 
                : List.of();
        
        // Convert to DTOs with quantity and value
        List<LocationInventoryDTO> dtos = pagedLocations.stream()
                .map(loc -> {
                    double qty = Optional.ofNullable(loc.getOnHand()).orElse(0.0);
                    double value = qty * currentPrice;
                    
                    return new LocationInventoryDTO(
                            loc.getLocation().getId(),
                            loc.getLocation().getName(),
                            qty,
                            value
                    );
                })
                .collect(Collectors.toList());
        
        // Create and return the Page object
        return new PageImpl<>(dtos, pageable, totalElements);
    }
}
