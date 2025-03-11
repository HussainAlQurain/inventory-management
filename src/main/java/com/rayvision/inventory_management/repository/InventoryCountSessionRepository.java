package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.InventoryCountSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCountSessionRepository extends JpaRepository<InventoryCountSession, Long> {

    List<InventoryCountSession> findByLocationId(Long locationId);
    Optional<InventoryCountSession> findByLocationIdAndId(Long locationId, Long sessionId);

    @Query("SELECT s FROM InventoryCountSession s WHERE s.location.id = :locationId AND s.locked = false")
    List<InventoryCountSession> findOpenSessionsByLocationId(@Param("locationId") Long locationId);

}
