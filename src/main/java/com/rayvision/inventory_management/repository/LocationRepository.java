package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCompanyIdAndId(Long companyId, Long id);
}
