package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.InventoryItemLocationMapper;
import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;
import com.rayvision.inventory_management.model.dto.ItemOnHandTotalsDTO;
import com.rayvision.inventory_management.model.dto.PageResponseDTO;
import com.rayvision.inventory_management.model.records.BulkUpdateRequest;
import com.rayvision.inventory_management.model.records.LocationInventoryDTO;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/inventory-item-locations")
public class InventoryItemLocationController {
    private final InventoryItemLocationService service;
    private final InventoryItemLocationMapper inventoryItemLocationMapper;

    public InventoryItemLocationController(InventoryItemLocationService service,
                                           InventoryItemLocationMapper inventoryItemLocationMapper) {
        this.service = service;
        this.inventoryItemLocationMapper = inventoryItemLocationMapper;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<InventoryItemLocationDTO> create(@RequestBody InventoryItemLocationDTO dto) {
        InventoryItemLocation created = service.create(dto);
        return new ResponseEntity<>(inventoryItemLocationMapper.toDto(created), HttpStatus.CREATED);
    }

    // GET
    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemLocationDTO> getOne(@PathVariable Long id) {
        InventoryItemLocation entity = service.getOne(id)
                .orElseThrow(() -> new RuntimeException("Not found bridging with id=" + id));
        return ResponseEntity.ok(inventoryItemLocationMapper.toDto(entity));
    }

    // UPDATE
    @PatchMapping("/{id}")
    public ResponseEntity<InventoryItemLocationDTO> partialUpdate(@PathVariable Long id,
                                                                  @RequestBody InventoryItemLocationDTO dto) {
        InventoryItemLocation updated = service.update(id, dto);
        return ResponseEntity.ok(inventoryItemLocationMapper.toDto(updated));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-update")
    public ResponseEntity<Void> bulkUpdate(@RequestBody BulkUpdateRequest req) {
        // example request = { "companyId": 123, "itemId": 456, "newMin": 10, "newPar": 20 }
        service.bulkUpdate(req.companyId(), req.itemId(), req.newMin(), req.newPar());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/item/{itemId}/company/{companyId}")
    public ResponseEntity<List<LocationInventoryDTO>> getLocationsForItem(
            @PathVariable Long itemId,
            @PathVariable Long companyId
    ) {
        // 1) fetch bridging for itemId
        List<InventoryItemLocation> bridgingList = service.findByItemId(itemId);

        // 2) filter bridging by company? i.e. bridgingList = bridgingList.stream()
        //        .filter(b -> b.getLocation().getCompany().getId().equals(companyId))
        //        .toList();

        // 3) build a DTO that has { location: {id,name}, quantity, value }
        //    Possibly compute 'value = quantity * item.currentPrice'
        //    For that, we might need the item's currentPrice or bridgingList might hold it.
        //    If you store item in bridging, you can do bridging.getInventoryItem().getCurrentPrice().

        List<LocationInventoryDTO> dtos = bridgingList.stream().map(b -> {
            double qty = Optional.ofNullable(b.getOnHand()).orElse(0.0);
            double price = 0.0;
            if (b.getInventoryItem() != null && b.getInventoryItem().getCurrentPrice() != null) {
                price = b.getInventoryItem().getCurrentPrice();
            }
            double value = qty * price;

            return new LocationInventoryDTO(
                    b.getLocation().getId(),
                    b.getLocation().getName(),
                    qty,
                    value
            );
        }).toList();

        return ResponseEntity.ok(dtos);
    }

    // NEW ENDPOINT: Get paginated location inventory for an item
    @GetMapping("/item/{itemId}/company/{companyId}/paginated")
    public ResponseEntity<PageResponseDTO<LocationInventoryDTO>> getPaginatedLocationsForItem(
            @PathVariable Long itemId,
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String locationSearch
    ) {
        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by("location.name").ascending());
        
        // Get paginated results
        Page<LocationInventoryDTO> pageResult = service.getPaginatedLocationsForItem(
                itemId, 
                companyId, 
                locationSearch, 
                pageable
        );
        
        // Convert to standard PageResponseDTO format
        PageResponseDTO<LocationInventoryDTO> response = new PageResponseDTO<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.hasNext(),
                pageResult.hasPrevious()
        );
        
        return ResponseEntity.ok(response);
    }

    // NEW ENDPOINT: Get on-hand totals for an item without fetching all locations
    @GetMapping("/item/{itemId}/company/{companyId}/totals")
    public ResponseEntity<ItemOnHandTotalsDTO> getItemOnHandTotals(
            @PathVariable Long itemId,
            @PathVariable Long companyId
    ) {
        ItemOnHandTotalsDTO totals = service.getItemOnHandTotals(itemId, companyId);
        return ResponseEntity.ok(totals);
    }

    @PutMapping("/items/{itemId}/locations/{locationId}/thresholds")
    public ResponseEntity<Void> setThresholdsForLocation(
            @PathVariable Long itemId,
            @PathVariable Long locationId,
            @RequestBody ThresholdUpdateRequest request
    ) {
        service.setThresholdsForLocation(itemId, locationId, request.minOnHand(), request.parLevel());
        return ResponseEntity.ok().build();
    }

    // 2. Bulk set thresholds for all locations in company
    @PutMapping("/items/{itemId}/companies/{companyId}/thresholds")
    public ResponseEntity<Void> bulkSetThresholdsForCompany(
            @PathVariable Long itemId,
            @PathVariable Long companyId,
            @RequestBody ThresholdUpdateRequest request
    ) {
        service.bulkSetThresholdsForCompany(companyId, itemId, request.minOnHand(), request.parLevel());
        return ResponseEntity.ok().build();
    }

    // 3. Partial update for thresholds (PATCH)
    @PatchMapping("/items/{itemId}/locations/{locationId}/thresholds")
    public ResponseEntity<Void> patchThresholds(
            @PathVariable Long itemId,
            @PathVariable Long locationId,
            @RequestBody ThresholdUpdateRequest request
    ) {
        service.patchThresholds(itemId, locationId, request.minOnHand(), request.parLevel());
        return ResponseEntity.ok().build();
    }

    // GET all InventoryItemLocations by itemId
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<InventoryItemLocationDTO>> getByItemId(@PathVariable Long itemId) {
        List<InventoryItemLocation> locations = service.findByItemId(itemId);
        List<InventoryItemLocationDTO> dtos = locations.stream()
                .map(inventoryItemLocationMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // GET InventoryItemLocation by itemId AND locationId
    @GetMapping("/item/{itemId}/location/{locationId}")
    public ResponseEntity<InventoryItemLocationDTO> getByItemIdAndLocationId(
            @PathVariable Long itemId,
            @PathVariable Long locationId
    ) {
        InventoryItemLocation entity = service.findByInventoryItemIdAndLocationId(itemId, locationId)
                .orElseThrow(() -> new RuntimeException(
                        "InventoryItemLocation not found for itemId=" + itemId + " and locationId=" + locationId
                ));
        return ResponseEntity.ok(inventoryItemLocationMapper.toDto(entity));
    }

    public record ThresholdUpdateRequest(Double minOnHand, Double parLevel) {}
}
