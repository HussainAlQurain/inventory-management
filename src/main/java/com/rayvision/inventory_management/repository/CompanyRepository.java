package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT c FROM Company c JOIN c.users u WHERE u.id = :userId")
    List<Company> findCompaniesByUserId(@Param("userId") Long userId);

    @Query("SELECT c.id FROM Company c JOIN c.users u WHERE u.id = :userId")
    List<Long> findCompaniesIdsByUserId(@Param("userId") Long userId);
}
