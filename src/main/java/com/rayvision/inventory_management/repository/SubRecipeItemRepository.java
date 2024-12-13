package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SubRecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubRecipeItemRepository extends JpaRepository<SubRecipeItem, Long> {
}
