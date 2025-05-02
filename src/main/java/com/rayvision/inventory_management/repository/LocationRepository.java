package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCompanyIdAndId(Long companyId, Long id);

    List<Location> findByCompanyId(Long companyId);
    
    /**
     * Find locations by company ID with pagination and optional search term
     * @param companyId Company ID
     * @param searchTerm Optional search term for filtering by name (case insensitive)
     * @param pageable Pagination parameters
     * @return Page of matching locations
     */
    @Query("""
        SELECT l FROM Location l
        WHERE l.company.id = :companyId
        AND (COALESCE(:searchTerm, '') = '' OR 
             LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<Location> findByCompanyIdWithSearch(
        @Param("companyId") Long companyId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );

    @Query("""
       SELECT l
         FROM Location l
         JOIN l.locationUsers lu
         WHERE l.company.id = :companyId
           AND lu.user.id = :userId
    """)
    List<Location> findByCompanyIdAndUserId(@Param("companyId") Long companyId,
                                            @Param("userId") Long userId);

    Optional<Location> findByIdAndCompanyId(Long locationId, Long companyId);

    List<Location> findByNameAndCompanyId(String name, Long companyId);
}
