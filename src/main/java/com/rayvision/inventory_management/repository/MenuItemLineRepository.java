package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.MenuItemLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemLineRepository extends JpaRepository<MenuItemLine, Long> {
}
