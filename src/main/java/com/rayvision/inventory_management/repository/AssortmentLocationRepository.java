package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.AssortmentLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssortmentLocationRepository extends JpaRepository<AssortmentLocation, Long> {

    List<AssortmentLocation> findByLocationId(Long locationId);
}
