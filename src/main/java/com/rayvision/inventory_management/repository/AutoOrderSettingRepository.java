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
    @Query("SELECT aos FROM AutoOrderSetting aos " +
           "LEFT JOIN FETCH aos.location loc " +
           "LEFT JOIN FETCH loc.company")
    List<AutoOrderSetting> findAllWithLocationAndCompany();
    
    /**
     * Find a specific setting with location and company eagerly fetched
     */
    @Query("SELECT aos FROM AutoOrderSetting aos " +
           "LEFT JOIN FETCH aos.location loc " +
           "LEFT JOIN FETCH loc.company " +
           "WHERE aos.id = :id")
    Optional<AutoOrderSetting> findByIdWithLocationAndCompany(@Param("id") Long id);

}
