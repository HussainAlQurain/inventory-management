package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.AutoRedistributeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoRedistributeSettingRepository extends JpaRepository<AutoRedistributeSetting, Long> {
    List<AutoRedistributeSetting> findByEnabledTrue();
}
