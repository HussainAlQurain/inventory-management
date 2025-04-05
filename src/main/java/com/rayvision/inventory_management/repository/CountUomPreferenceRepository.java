package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.CountUomPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountUomPreferenceRepository extends JpaRepository<CountUomPreference, Long> {
    List<CountUomPreference> findByInventoryItemId(Long inventoryItemId);
    List<CountUomPreference> findBySubRecipeId(Long subRecipeId);
}
