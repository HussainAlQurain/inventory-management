package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SubRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubRecipeRepository extends JpaRepository<SubRecipe, Long> {
}
