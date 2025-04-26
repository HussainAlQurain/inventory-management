package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.AutoOrderSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoOrderSettingRepository extends JpaRepository<AutoOrderSetting, Long> {
    Optional<AutoOrderSetting> findByLocationId(Long locationId);
    
    /**
     * Find all settings with location and company eagerly fetched
     * to avoid LazyInitializationException in async processing
     */
    @Query("SELECT a FROM AutoOrderSetting a JOIN FETCH a.location l JOIN FETCH l.company")
    List<AutoOrderSetting> findAllWithLocationAndCompany();
    
    /**
     * Find a specific setting with location and company eagerly fetched
     */
    @Query("SELECT a FROM AutoOrderSetting a JOIN FETCH a.location l JOIN FETCH l.company WHERE a.id = :id")
    Optional<AutoOrderSetting> findByIdWithLocationAndCompany(@Param("id") Long id);

}
