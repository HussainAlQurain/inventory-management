package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.AutoOrderSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoOrderSettingRepository extends JpaRepository<AutoOrderSetting, Long> {
    Optional<AutoOrderSetting> findByLocationId(Long locationId);
}
