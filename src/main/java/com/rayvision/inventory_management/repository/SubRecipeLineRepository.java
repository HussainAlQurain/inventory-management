package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SubRecipeLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubRecipeLineRepository extends JpaRepository<SubRecipeLine, Long> {

}
