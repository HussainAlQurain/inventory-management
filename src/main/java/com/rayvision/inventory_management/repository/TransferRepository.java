package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /* outgoing drafts created by a location */
    List<Transfer> findByFromLocationIdAndStatus(Long locId, String status);

    /* incoming drafts waiting to be fulfilled by a location */
    List<Transfer> findByToLocationIdAndStatus(Long locId, String status);

    /* Find transfers by status */
    List<Transfer> findByStatus(String status);

    /* company‑wide views --------------------------------------------------- */
    @Query("""
       select t from Transfer t
       where t.fromLocation.company.id = :companyId
         and t.status = :status
    """)
    List<Transfer> findOutgoingDraftsByCompany(Long companyId, String status);

    @Query("""
       select t from Transfer t
       where t.toLocation.company.id = :companyId
         and t.status = :status
    """)
    List<Transfer> findIncomingDraftsByCompany(Long companyId, String status);

    @Query("""
       select t from Transfer t
       where (t.fromLocation.company.id = :companyId
          or t.toLocation.company.id = :companyId)
         and t.status = :status
    """)
    List<Transfer> findCompletedTransfersByCompany(@Param("companyId") Long companyId,
                                                   @Param("status") String status);

    Optional<Transfer> findFirstByFromLocationIdAndToLocationIdAndStatus(
            Long fromId, Long toId, String status);


    @Query("""
    SELECT t
    FROM   Transfer t
    LEFT JOIN FETCH t.lines
    WHERE  t.id = :id
""")
    Optional<Transfer> findByIdWithLines(@Param("id") Long id);


}
