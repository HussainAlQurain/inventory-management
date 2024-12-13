package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergenRepository extends JpaRepository<Allergen, Long> {
}
