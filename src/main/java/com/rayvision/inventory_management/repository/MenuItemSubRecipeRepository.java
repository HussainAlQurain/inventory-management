package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.MenuItemSubRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemSubRecipeRepository extends JpaRepository<MenuItemSubRecipe, Long> {
}
