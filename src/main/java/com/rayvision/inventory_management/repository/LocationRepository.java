package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Location;
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
