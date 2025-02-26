package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.InventoryCountLine;
import com.rayvision.inventory_management.model.InventoryCountSession;
import com.rayvision.inventory_management.service.InventoryCountSessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations/{locationId}/inventory-count-sessions")
public class InventoryCountSessionController {

    private final InventoryCountSessionService sessionService;

    public InventoryCountSessionController(InventoryCountSessionService sessionService) {
        this.sessionService = sessionService;
    }

    // CREATE SESSION
    @PostMapping
    public InventoryCountSession createSession(@PathVariable Long locationId,
                                               @RequestBody InventoryCountSession dto) {
        return sessionService.createSession(locationId, dto);
    }

    // GET ONE SESSION
    @GetMapping("/{sessionId}")
    public InventoryCountSession getSession(@PathVariable Long locationId,
                                            @PathVariable Long sessionId) {
        // optional check if location matches
        return sessionService.getSession(sessionId);
    }

    // UPDATE SESSION
    @PatchMapping("/{sessionId}")
    public InventoryCountSession patchSession(@PathVariable Long locationId,
                                              @PathVariable Long sessionId,
                                              @RequestBody InventoryCountSession dto) {
        return sessionService.updateSession(sessionId, dto);
    }

    // LOCK SESSION
    @PostMapping("/{sessionId}/lock")
    public InventoryCountSession lockSession(@PathVariable Long locationId,
                                             @PathVariable Long sessionId) {
        return sessionService.lockSession(sessionId);
    }

    // UNLOCK SESSION
    @PostMapping("/{sessionId}/unlock")
    public InventoryCountSession unlockSession(@PathVariable Long locationId,
                                               @PathVariable Long sessionId) {
        return sessionService.unlockSession(sessionId);
    }

    // DELETE SESSION
    @DeleteMapping("/{sessionId}")
    public void deleteSession(@PathVariable Long locationId,
                              @PathVariable Long sessionId) {
        sessionService.deleteSession(sessionId);
    }

    // -----------------------------------------------------------
    // LINES
    // -----------------------------------------------------------
    @PostMapping("/{sessionId}/lines")
    public InventoryCountLine addLine(@PathVariable Long locationId,
                                      @PathVariable Long sessionId,
                                      @RequestBody InventoryCountLine lineDto) {
        return sessionService.addLine(sessionId, lineDto);
    }

    @PatchMapping("/{sessionId}/lines/{lineId}")
    public InventoryCountLine updateLine(@PathVariable Long locationId,
                                         @PathVariable Long sessionId,
                                         @PathVariable Long lineId,
                                         @RequestBody InventoryCountLine lineDto) {
        return sessionService.updateLine(sessionId, lineId, lineDto);
    }

    @DeleteMapping("/{sessionId}/lines/{lineId}")
    public void deleteLine(@PathVariable Long locationId,
                           @PathVariable Long sessionId,
                           @PathVariable Long lineId) {
        sessionService.deleteLine(sessionId, lineId);
    }

    // If you want to get lines for a session
    @GetMapping("/{sessionId}/lines")
    public List<InventoryCountLine> getLines(@PathVariable Long locationId,
                                             @PathVariable Long sessionId) {
        InventoryCountSession session = sessionService.getSession(sessionId);
        return session.getLines().stream().collect(Collectors.toList());
    }

}
