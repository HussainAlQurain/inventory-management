package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /* outgoing drafts created by a location */
    List<Transfer> findByFromLocationIdAndStatus(Long locId, String status);

    /* incoming drafts waiting to be fulfilled by a location */
    List<Transfer> findByToLocationIdAndStatus(Long locId, String status);

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

}
