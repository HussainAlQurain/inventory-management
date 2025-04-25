package com.rayvision.inventory_management.repository;


import com.rayvision.inventory_management.model.CompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyUserRepository extends JpaRepository<CompanyUser, Long> {
    List<CompanyUser> findByCompanyId(Long companyId);

    List<CompanyUser> findByUserId(Long userId);

    @Query("SELECT cu.company.id FROM CompanyUser cu WHERE cu.user.id = :userId")
    List<Long> findCompanyIdsByUserId(@Param("userId") Long userId);

    Optional<CompanyUser> findByCompanyIdAndUserId(Long companyId, Long userId);
    
    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);
}
