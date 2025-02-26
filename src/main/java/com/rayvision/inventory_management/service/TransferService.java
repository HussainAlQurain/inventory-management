package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;

public interface TransferService {
    Transfer createTransfer(TransferCreateDTO dto);
    Transfer completeTransfer(Long transferId); // finalize the transfer, record stock transactions
    Transfer getTransfer(Long transferId);
    void deleteTransfer(Long transferId);
}
