package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByCompanyId(Long companyId);
    Optional<Category> findByCompanyIdAndId(Long companyId, Long id);
}
