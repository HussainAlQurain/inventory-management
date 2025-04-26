package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.enums.TransferStatus;
import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for inventory transfers between locations
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /* outgoing drafts created by a location */
    List<Transfer> findByFromLocationIdAndStatus(Long locId, TransferStatus status);

    /* incoming drafts waiting to be fulfilled by a location */
    List<Transfer> findByToLocationIdAndStatus(Long locId, TransferStatus status);

    /* Find transfers by status */
    List<Transfer> findByStatus(TransferStatus status);

    /* company‑wide views --------------------------------------------------- */
    @Query("""
       select t from Transfer t
       where t.fromLocation.company.id = :companyId
         and t.status = :status
    """)
    List<Transfer> findOutgoingDraftsByCompany(Long companyId, TransferStatus status);

    @Query("""
       select t from Transfer t
       where t.toLocation.company.id = :companyId
         and t.status = :status
    """)
    List<Transfer> findIncomingDraftsByCompany(Long companyId, TransferStatus status);

    @Query("""
       select t from Transfer t
       where (t.fromLocation.company.id = :companyId
          or t.toLocation.company.id = :companyId)
         and t.status = :status
    """)
    List<Transfer> findCompletedTransfersByCompany(@Param("companyId") Long companyId,
                                                   @Param("status") TransferStatus status);

    Optional<Transfer> findFirstByFromLocationIdAndToLocationIdAndStatus(
            Long fromId, Long toId, TransferStatus status);


    @Query("""
    SELECT t
    FROM   Transfer t
    LEFT JOIN FETCH t.lines
    WHERE  t.id = :id
""")
    Optional<Transfer> findByIdWithLines(@Param("id") Long id);

    /**
     * Get quantities for items that are scheduled to be transferred to a location.
     * Returns arrays of [itemId, quantityToBeReceived]
     * 
     * This query calculates quantities for items in transfers that are not CANCELED or COMPLETED
     */
    @Query("SELECT l.inventoryItem.id, SUM(l.quantity) " +
           "FROM Transfer t " +
           "JOIN t.lines l " +
           "WHERE t.toLocation.id = :locationId " +
           "AND t.status <> 'CANCELLED' AND t.status <> 'COMPLETED' " +
           "GROUP BY l.inventoryItem.id")
    List<Object[]> getIncomingTransferQuantitiesByLocation(@Param("locationId") Long locationId);
    
    /**
     * Qty already LEAVING a location but not yet received.
     * (DRAFT or SENT transfers)
     */
    @Query("""
    SELECT tl.inventoryItem.id,
           t.fromLocation.id,
           SUM(tl.quantity)
    FROM   Transfer t
    JOIN   t.lines tl
    WHERE  t.status IN ('DRAFT','SENT')
    GROUP  BY tl.inventoryItem.id, t.fromLocation.id
    """)
    List<Object[]> getOutgoingQty();

    /**
     * Qty already COMING IN to a location but not yet received.
     */
    @Query("""
    SELECT tl.inventoryItem.id,
           t.toLocation.id,
           SUM(tl.quantity)
    FROM   Transfer t
    JOIN   t.lines tl
    WHERE  t.status IN ('DRAFT','SENT')
    GROUP  BY tl.inventoryItem.id, t.toLocation.id
    """)
    List<Object[]> getIncomingQty();
    
    /**
     * Find system draft for cleanup purposes
     */
    @Query("""
    FROM Transfer t
    WHERE t.status   = 'DRAFT'
      AND t.createdByUser.id = :sysId
      AND t.fromLocation.id  = :fromId
      AND t.toLocation.id    = :toId
    """)
    Optional<Transfer> findSystemDraft(Long sysId, Long fromId, Long toId);
    
    /**
     * Find all draft transfers created by a specific user for a company
     */
    @Query("""
    SELECT t FROM Transfer t
    WHERE t.status = :status
    AND t.createdByUser = :user
    AND (t.fromLocation.company.id = :companyId OR t.toLocation.company.id = :companyId)
    """)
    List<Transfer> findAllByCompanyAndStatusAndCreatedByUser(Long companyId, String status, Users user);
}
