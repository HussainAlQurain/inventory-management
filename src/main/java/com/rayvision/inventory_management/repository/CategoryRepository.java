package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.dto.FilterOptionDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByCompanyId(Long companyId);
    Optional<Category> findByCompanyIdAndId(Long companyId, Long id);

    @Query("""
    SELECT DISTINCT c
    FROM Category c
    LEFT JOIN c.inventoryItems i
    LEFT JOIN c.subRecipes sr
    LEFT JOIN c.menuItems mi
    LEFT JOIN c.suppliers s
    WHERE c.company.id = :companyId
      AND (:searchTerm = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
      AND (
         i IS NOT NULL
         OR (i IS NULL AND sr IS NULL AND mi IS NULL AND s IS NULL)
      )
""")
    List<Category> searchForInventoryItemsOrUnused(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm
    );


    // Add this to CategoryRepository.java
    @Query("SELECT new com.rayvision.inventory_management.model.dto.FilterOptionDTO(c.id, c.name) " +
            "FROM Category c WHERE c.company.id = :companyId " +
            "AND (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.name")
    List<FilterOptionDTO> findFilterOptions(
            @Param("companyId") Long companyId,
            @Param("search") String search);
}
