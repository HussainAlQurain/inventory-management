package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.UomFilterOptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    List<UnitOfMeasure> findByCompanyId(Long companyId);
    Optional<UnitOfMeasure> findByCompanyIdAndId(Long companyId, Long id);
    Optional<UnitOfMeasure> findByCompanyIdAndAbbreviation(Long companyId, String abbreviation);

    @Query("""
       SELECT u
         FROM UnitOfMeasure u
         WHERE u.category.id = :categoryId
         AND u.company.id = :companyId
    """)
    List<UnitOfMeasure> findByCategoryAndCompany(@Param("categoryId") Long categoryId,
                                                 @Param("companyId") Long companyId);

    // Add this method
    @Query("SELECT new com.rayvision.inventory_management.model.dto.UomFilterOptionDTO(u.id, u.name, u.abbreviation) " +
            "FROM UnitOfMeasure u WHERE u.company.id = :companyId " +
            "AND (:search = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.abbreviation) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.name")
    Page<UomFilterOptionDTO> findPaginatedFilterOptions(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable);
}
