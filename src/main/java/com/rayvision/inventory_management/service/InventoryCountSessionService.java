package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryCountLine;
import com.rayvision.inventory_management.model.InventoryCountSession;
import com.rayvision.inventory_management.model.dto.InventoryCountSessionDTO;
import com.rayvision.inventory_management.model.dto.InventoryCountSessionSummaryDTO;

import java.time.LocalDate;
import java.util.List;

public interface InventoryCountSessionService {


    InventoryCountSession createSession(Long locationId, InventoryCountSessionDTO dto);

    InventoryCountSession getSession(Long sessionId);

    InventoryCountSession updateSession(Long sessionId, InventoryCountSession dto);

    void deleteSession(Long sessionId);

    InventoryCountSession lockSession(Long sessionId);

    InventoryCountSession unlockSession(Long sessionId);

    // LINES
    InventoryCountLine addLine(Long sessionId, InventoryCountLine line);

    InventoryCountLine updateLine(Long sessionId, Long lineId, InventoryCountLine patchLine);

    void deleteLine(Long sessionId, Long lineId);

    List<InventoryCountSessionSummaryDTO> findByCompanyAndDateRange(Long companyId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate);


}
