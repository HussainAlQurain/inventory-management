package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.service.impl.RedistributeJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransferService {
    Transfer createTransfer(TransferCreateDTO dto);
    /** receiving‑location may update any draft lines before completing */
    Transfer updateDraft(Long transferId, List<TransferLineDTO> newLines,
                         Long actingLocationId);
    Transfer completeTransfer(Long transferId); // finalize the transfer, record stock transactions

    /* look‑ups ----------------------------------------------------------- */
    List<Transfer> findOutgoingDraftsByLocation(Long locationId);
    List<Transfer> findIncomingDraftsByLocation(Long locationId);

    List<Transfer> findOutgoingDraftsByCompany(Long companyId);
    List<Transfer> findIncomingDraftsByCompany(Long companyId);
    
    /* Completed transfers look-ups --------------------------------------- */
    List<Transfer> findCompletedTransfersByCompany(Long companyId);
    List<Transfer> findCompletedTransfersByLocation(Long locationId, boolean isFromLocation);
    List<Transfer> findAllCompletedTransfers();

    Transfer getTransfer(Long transferId);
    void deleteTransfer(Long transferId);

    /** return *one* draft between two locations or <code>null</code>         */
    Transfer findDraftBetween(Long fromLocationId, Long toLocationId);

    /** merge shortages into an existing draft                                */
    Transfer updateDraftWithLines(Transfer draft,
                                  List<RedistributeJob.ShortLine> lines,
                                  String comment);

    /* ───── TransferService */
    Page<Transfer> findOutgoingDraftsByCompanyPaginated(
            Long companyId, Long locationId,
            String searchTerm, Pageable pageable);

    Page<Transfer> findIncomingDraftsByCompanyPaginated(
            Long companyId, Long locationId,
            String searchTerm, Pageable pageable);

    Page<Transfer> findCompletedTransfersByCompanyPaginated(
            Long companyId, Long locationId, boolean fromLocation,
            String searchTerm, Pageable pageable);
}
