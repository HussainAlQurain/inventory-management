package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.AutoRedistributeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoRedistributeSettingRepository extends JpaRepository<AutoRedistributeSetting, Long> {
    List<AutoRedistributeSetting> findByEnabledTrue();

    /**
     * Find all enabled settings with company eagerly fetched
     * to avoid LazyInitializationException in async processing
     */
    @Query("SELECT ars FROM AutoRedistributeSetting ars " +
           "LEFT JOIN FETCH ars.company " +
           "WHERE ars.enabled = true")
    List<AutoRedistributeSetting> findByEnabledTrueWithCompany();
    
    /**
     * Find one setting by ID with company eagerly fetched
     */
    @Query("SELECT ars FROM AutoRedistributeSetting ars " +
           "LEFT JOIN FETCH ars.company " +
           "WHERE ars.id = :id")
    Optional<AutoRedistributeSetting> findByIdWithCompany(Long id);

    /**
     * Find setting by company ID with company eagerly fetched
     * to avoid LazyInitializationException
     */
    @Query("SELECT ars FROM AutoRedistributeSetting ars " +
           "LEFT JOIN FETCH ars.company c " +
           "WHERE c.id = :companyId")
    Optional<AutoRedistributeSetting> findByCompanyId(@Param("companyId") Long companyId);
}
