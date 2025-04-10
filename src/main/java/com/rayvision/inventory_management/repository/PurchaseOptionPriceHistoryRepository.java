package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.PurchaseOptionPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOptionPriceHistoryRepository extends JpaRepository<PurchaseOptionPriceHistory, Long> {
}
