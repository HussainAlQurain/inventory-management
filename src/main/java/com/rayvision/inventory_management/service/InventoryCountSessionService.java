package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryCountLine;
import com.rayvision.inventory_management.model.InventoryCountSession;

public interface InventoryCountSessionService {
    InventoryCountSession createSession(Long locationId, InventoryCountSession session);
    InventoryCountSession getSession(Long sessionId);
    InventoryCountSession updateSession(Long sessionId, InventoryCountSession session);
    void deleteSession(Long sessionId);

    // Lock/unlock
    InventoryCountSession lockSession(Long sessionId);
    InventoryCountSession unlockSession(Long sessionId);

    // Lines
    InventoryCountLine addLine(Long sessionId, InventoryCountLine line);
    InventoryCountLine updateLine(Long sessionId, Long lineId, InventoryCountLine line);
    void deleteLine(Long sessionId, Long lineId);

}
