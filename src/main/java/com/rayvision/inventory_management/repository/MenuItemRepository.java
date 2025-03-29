package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @Query("""
    SELECT mi
    FROM MenuItem mi
    WHERE mi.company.id = :companyId
      AND (:searchTerm = '' 
           OR LOWER(mi.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    List<MenuItem> searchMenuItems(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm
    );

    List<MenuItem> findByCompanyId(Long companyId);
    Optional<MenuItem>  findByIdAndCompanyId(Long id, Long companyId);
}
