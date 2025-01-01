package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository <Orders, Long> {
}
