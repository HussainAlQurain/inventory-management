package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    // Example: find all transactions for a location + item in date range
    List<StockTransaction> findByLocationIdAndItemIdAndDateBetween(Long locationId, Long itemId, LocalDate start, LocalDate end);

    // Possibly other queries as needed
    List<StockTransaction> findByLocationIdAndItemId(Long locationId, Long itemId);
    List<StockTransaction> findBySourceReferenceId(Long sourceReferenceId);

    List<StockTransaction> findBySourceReferenceIdAndTransactionType(Long refId, String type);

    // Returns all StockTransaction records for this locationId + itemId
    // where date <= upToDate
    List<StockTransaction> findByLocationIdAndItemIdAndDateLessThanEqual(
            Long locationId,
            Long itemId,
            LocalDate upToDate
    );

}
