package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.PurchaseOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOptionRepository extends JpaRepository<PurchaseOption, Long> {
    List<PurchaseOption> findByInventoryItemId(Long inventoryItemId);

    @Query("""
           SELECT po
             FROM PurchaseOption po
             JOIN po.inventoryItem i
             WHERE i.company.id = :companyId
           """)
    List<PurchaseOption> findAllByCompanyId(@Param("companyId") Long companyId);

}
