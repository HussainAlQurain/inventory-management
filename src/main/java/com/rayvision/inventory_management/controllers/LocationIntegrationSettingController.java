package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.LocationIntegrationSetting;
import com.rayvision.inventory_management.model.dto.IntegrationSettingDTO;
import com.rayvision.inventory_management.repository.LocationIntegrationSettingRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/integration-settings")
public class LocationIntegrationSettingController {
    private final LocationIntegrationSettingRepository repo;
    private final LocationRepository locationRepository;
    // or however you fetch your locations

    public LocationIntegrationSettingController(
            LocationIntegrationSettingRepository repo,
            LocationRepository locationRepository
    ) {
        this.repo = repo;
        this.locationRepository = locationRepository;
    }

    /**
     * List all integration settings
     */
    @GetMapping
    public List<IntegrationSettingDTO> getAll() {
        List<LocationIntegrationSetting> settings = repo.findAll();
        return settings.stream().map(this::toDto).toList();
    }

    /**
     * Get single location’s setting
     */
    @GetMapping("/{locationId}")
    public ResponseEntity<IntegrationSettingDTO> getSettingByLocation(@PathVariable Long locationId) {
        Optional<LocationIntegrationSetting> opt = repo.findByLocationId(locationId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(opt.get()));
    }

    /**
     * Update or create if not exist
     */
    @PutMapping("/{locationId}")
    public ResponseEntity<IntegrationSettingDTO> updateSetting(
            @PathVariable Long locationId,
            @RequestBody IntegrationSettingDTO dto
    ) {
        // ensure location exists
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        // find or create
        LocationIntegrationSetting setting = repo.findByLocationId(locationId)
                .orElse(new LocationIntegrationSetting(null, loc, null, 300, false, false,
                        null, 0, null, 0, null));

        // update fields from dto
        setting.setPosApiUrl(dto.getPosApiUrl());
        setting.setFrequentSyncSeconds(dto.getFrequentSyncSeconds());
        setting.setFrequentSyncEnabled(dto.isFrequentSyncEnabled());
        setting.setDailySyncEnabled(dto.isDailySyncEnabled());
        // save
        LocationIntegrationSetting saved = repo.save(setting);

        return ResponseEntity.ok(toDto(saved));
    }

    private IntegrationSettingDTO toDto(LocationIntegrationSetting s) {
        IntegrationSettingDTO dto = new IntegrationSettingDTO();
        dto.setLocationId(s.getLocation().getId());
        dto.setPosApiUrl(s.getPosApiUrl());
        dto.setFrequentSyncSeconds(s.getFrequentSyncSeconds());
        dto.setFrequentSyncEnabled(s.isFrequentSyncEnabled());
        dto.setDailySyncEnabled(s.isDailySyncEnabled());
        return dto;
    }
}
