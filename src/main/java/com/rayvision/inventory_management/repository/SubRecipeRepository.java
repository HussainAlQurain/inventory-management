package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SubRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubRecipeRepository extends JpaRepository<SubRecipe, Long> {
    List<SubRecipe> findByCompanyId(Long companyId);

    @Query("""
    SELECT sr
    FROM SubRecipe sr
    WHERE sr.company.id = :companyId
      AND (:searchTerm = '' 
           OR LOWER(sr.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    List<SubRecipe> searchSubRecipes(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm
    );

}
