package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.dto.FilterOptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByCompanyId(Long companyId);
    Optional<Supplier> findByCompanyIdAndId(Long companyId, Long id);
    @Query("SELECT s FROM Supplier s WHERE s.company.id = :companyId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Supplier> findByCompanyIdAndNameContaining(@Param("companyId") Long companyId,
                                                    @Param("searchTerm") String searchTerm);

    // New paginated methods
    Page<Supplier> findByCompanyId(Long companyId, Pageable pageable);
    
    @Query("SELECT s FROM Supplier s WHERE s.company.id = :companyId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Supplier> findByCompanyIdAndNameContaining(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    // Add this to SupplierRepository.java
    @Query("SELECT new com.rayvision.inventory_management.model.dto.FilterOptionDTO(s.id, s.name) " +
            "FROM Supplier s WHERE s.company.id = :companyId " +
            "AND (:search = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY s.name")
    List<FilterOptionDTO> findFilterOptions(
            @Param("companyId") Long companyId,
            @Param("search") String search);

    @Query("SELECT new com.rayvision.inventory_management.model.dto.FilterOptionDTO(s.id, s.name) " +
            "FROM Supplier s WHERE s.company.id = :companyId " +
            "AND (:search = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY s.name")
    Page<FilterOptionDTO> findPaginatedFilterOptions(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable);
}
