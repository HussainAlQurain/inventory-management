package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;

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

    Transfer getTransfer(Long transferId);
    void deleteTransfer(Long transferId);
}
