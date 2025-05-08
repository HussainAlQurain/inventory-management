package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.enums.SubRecipeType;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.SubRecipeListDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubRecipeRepository extends JpaRepository<SubRecipe, Long>, JpaSpecificationExecutor<SubRecipe> {
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

    List<SubRecipe> findByCompanyIdAndType(Long companyId, SubRecipeType type);

    // New paginated methods
    Page<SubRecipe> findByCompanyId(Long companyId, Pageable pageable);
    
    @Query("""
    SELECT sr
    FROM SubRecipe sr
    WHERE sr.company.id = :companyId
      AND (:searchTerm = '' 
           OR LOWER(sr.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<SubRecipe> searchSubRecipes(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
    
    Page<SubRecipe> findByCompanyIdAndType(Long companyId, SubRecipeType type, Pageable pageable);

    @Query("""
    SELECT new com.rayvision.inventory_management.model.dto.SubRecipeListDTO(
        sr.id, sr.name, sr.type, 
        c.id, c.name,
        u.id, u.name, u.abbreviation,
        sr.yieldQty, sr.cost)
    FROM SubRecipe sr
    LEFT JOIN sr.category c
    LEFT JOIN sr.uom u
    WHERE sr.company.id = :companyId
    AND (:searchTerm = '' 
         OR LOWER(sr.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<SubRecipeListDTO> searchSubRecipesLight(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    @Query("""
    SELECT new com.rayvision.inventory_management.model.dto.SubRecipeListDTO(
           sr.id, sr.name, sr.type,
           cat.id, cat.name,
           u.id,  u.name, u.abbreviation,
           sr.yieldQty, sr.cost)
    FROM   SubRecipe sr
           JOIN sr.category  cat
           JOIN sr.uom       u
    WHERE  sr.company.id = :companyId
      AND (:search IS NULL OR LOWER(sr.name) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (:catId  IS NULL OR cat.id = :catId)
      AND (:rType  IS NULL OR sr.type = :rType)
    """)
    Page<SubRecipeListDTO> findGridSlice(@Param("companyId") Long companyId,
                                         @Param("search")     String search,
                                         @Param("catId")      Long   catId,
                                         @Param("rType")      SubRecipeType rType,
                                         Pageable pageable);



    @EntityGraph(attributePaths = {
            "category",
            "unitOfMeasure",
            "lines",
            "lines.inventoryItem",
            "lines.inventoryItem.defaultUom",
            "lines.childSubRecipe",
            "lines.unitOfMeasure"
    })
    Optional<SubRecipe> findByCompanyIdAndId(Long companyId, Long id);


}
