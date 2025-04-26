package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.AutoOrderSetting;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.dto.AutoOrderSettingDTO;
import com.rayvision.inventory_management.repository.AutoOrderSettingRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.service.impl.DynamicSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/auto-order-settings")
public class AutoOrderSettingController {
    private final AutoOrderSettingRepository repo;
    private final LocationRepository locationRepository;
    private final DynamicSchedulerService schedulerService;

    public AutoOrderSettingController(
            AutoOrderSettingRepository repo, 
            LocationRepository locationRepository,
            DynamicSchedulerService schedulerService) {
        this.repo = repo;
        this.locationRepository = locationRepository;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public List<AutoOrderSettingDTO> getAllSettings() {
        List<AutoOrderSetting> settings = repo.findAll();
        return settings.stream().map(this::toDto).toList();
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<AutoOrderSettingDTO> getSetting(@PathVariable Long locationId) {
        Optional<AutoOrderSetting> opt = repo.findByLocationId(locationId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(opt.get()));
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<AutoOrderSettingDTO> updateSetting(
            @PathVariable Long locationId,
            @RequestBody AutoOrderSettingDTO dto
    ) {
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        AutoOrderSetting setting = repo.findByLocationId(locationId)
                .orElse(new AutoOrderSetting(null, loc, false, 300,
                        null, null, null));
        // update
        setting.setEnabled(dto.isEnabled());
        setting.setFrequencySeconds(dto.getFrequencySeconds());
        setting.setSystemUserId(dto.getSystemUserId());
        setting.setAutoOrderComment(dto.getAutoOrderComment());

        AutoOrderSetting saved = repo.save(setting);
        
        // Refresh scheduler after settings change
        schedulerService.refreshAllJobs();
        
        return ResponseEntity.ok(toDto(saved));
    }

    private AutoOrderSettingDTO toDto(AutoOrderSetting s) {
        AutoOrderSettingDTO dto = new AutoOrderSettingDTO();
        dto.setLocationId(s.getLocation().getId());
        dto.setEnabled(s.isEnabled());
        dto.setFrequencySeconds(s.getFrequencySeconds());
        dto.setSystemUserId(s.getSystemUserId());
        dto.setAutoOrderComment(s.getAutoOrderComment());
        return dto;
    }
}
