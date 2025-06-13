package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.PrepItemLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrepItemLocationRepository extends JpaRepository<PrepItemLocation, Long> {
    List<PrepItemLocation> findBySubRecipeId(Long subRecipeId);
    List<PrepItemLocation> findByLocationId(Long locationId);
    Optional<PrepItemLocation> findBySubRecipeIdAndLocationId(Long subRecipeId, Long locationId);

}
