package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.LocationIntegrationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationIntegrationSettingRepository extends JpaRepository<LocationIntegrationSetting, Long> {
    Optional<LocationIntegrationSetting> findByLocationId(Long locationId);
}
