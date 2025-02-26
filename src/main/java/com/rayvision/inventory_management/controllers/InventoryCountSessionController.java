package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.InventoryCountSessionMapper;
import com.rayvision.inventory_management.model.InventoryCountLine;
import com.rayvision.inventory_management.model.InventoryCountSession;
import com.rayvision.inventory_management.model.dto.InventoryCountLineDTO;
import com.rayvision.inventory_management.model.dto.InventoryCountSessionDTO;
import com.rayvision.inventory_management.service.InventoryCountSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations/{locationId}/inventory-count-sessions")
public class InventoryCountSessionController {

    private final InventoryCountSessionService sessionService;
    private final InventoryCountSessionMapper sessionMapper;

    public InventoryCountSessionController(InventoryCountSessionService sessionService,
                                           InventoryCountSessionMapper sessionMapper) {
        this.sessionService = sessionService;
        this.sessionMapper = sessionMapper;
    }

    // CREATE SESSION (with lines auto-populated for all items)
    @PostMapping
    public ResponseEntity<InventoryCountSessionDTO> createSession(
            @PathVariable Long locationId,
            @RequestBody InventoryCountSessionDTO dto
    ) {
        InventoryCountSession createdSession = sessionService.createSession(locationId, dto);
        InventoryCountSessionDTO responseDTO = sessionMapper.toDto(createdSession);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    // GET ONE SESSION
    @GetMapping("/{sessionId}")
    public ResponseEntity<InventoryCountSessionDTO> getSession(
            @PathVariable Long locationId,
            @PathVariable Long sessionId
    ) {
        // Optionally confirm the location matches
        InventoryCountSession session = sessionService.getSession(sessionId);
        return ResponseEntity.ok(sessionMapper.toDto(session));
    }

    // PARTIAL UPDATE or FULL (we do patch for partial)
    @PatchMapping("/{sessionId}")
    public ResponseEntity<InventoryCountSessionDTO> patchSession(
            @PathVariable Long locationId,
            @PathVariable Long sessionId,
            @RequestBody InventoryCountSessionDTO dto
    ) {
        // We’re using the entity approach from the service: “updateSession(sessionId, entity)”
        // So we convert the minimal DTO -> entity
        InventoryCountSession patchEntity = new InventoryCountSession();
        // set fields from DTO
        patchEntity.setCountDate(dto.getCountDate());
        patchEntity.setDayPart(dto.getDayPart());
        patchEntity.setDescription(dto.getDescription());

        InventoryCountSession updated = sessionService.updateSession(sessionId, patchEntity);
        return ResponseEntity.ok(sessionMapper.toDto(updated));
    }

    // LOCK SESSION
    @PostMapping("/{sessionId}/lock")
    public ResponseEntity<InventoryCountSessionDTO> lockSession(
            @PathVariable Long locationId,
            @PathVariable Long sessionId
    ) {
        InventoryCountSession locked = sessionService.lockSession(sessionId);
        return ResponseEntity.ok(sessionMapper.toDto(locked));
    }

    // UNLOCK SESSION
    @PostMapping("/{sessionId}/unlock")
    public ResponseEntity<InventoryCountSessionDTO> unlockSession(
            @PathVariable Long locationId,
            @PathVariable Long sessionId
    ) {
        InventoryCountSession unlocked = sessionService.unlockSession(sessionId);
        return ResponseEntity.ok(sessionMapper.toDto(unlocked));
    }

    // DELETE SESSION
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long locationId,
            @PathVariable Long sessionId
    ) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------
    // LINES
    // -----------------------------------------------------------
    @PostMapping("/{sessionId}/lines")
    public ResponseEntity<InventoryCountLineDTO> addLine(
            @PathVariable Long locationId,
            @PathVariable Long sessionId,
            @RequestBody InventoryCountLineDTO lineDto
    ) {
        // Convert DTO -> entity
        InventoryCountLine entity = sessionMapper.toLineEntity(lineDto);
        // service call
        InventoryCountLine saved = sessionService.addLine(sessionId, entity);
        // Convert back to DTO
        InventoryCountLineDTO response = sessionMapper.toLineDto(saved);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{sessionId}/lines/{lineId}")
    public ResponseEntity<InventoryCountLineDTO> updateLine(
            @PathVariable Long locationId,
            @PathVariable Long sessionId,
            @PathVariable Long lineId,
            @RequestBody InventoryCountLineDTO lineDto
    ) {
        InventoryCountLine patchEntity = sessionMapper.toLineEntity(lineDto);
        InventoryCountLine updated = sessionService.updateLine(sessionId, lineId, patchEntity);
        return ResponseEntity.ok(sessionMapper.toLineDto(updated));
    }

    @DeleteMapping("/{sessionId}/lines/{lineId}")
    public ResponseEntity<Void> deleteLine(
            @PathVariable Long locationId,
            @PathVariable Long sessionId,
            @PathVariable Long lineId
    ) {
        sessionService.deleteLine(sessionId, lineId);
        return ResponseEntity.noContent().build();
    }

    // GET lines for a session
    @GetMapping("/{sessionId}/lines")
    public ResponseEntity<?> getLines(
            @PathVariable Long locationId,
            @PathVariable Long sessionId
    ) {
        InventoryCountSession session = sessionService.getSession(sessionId);
        var lineDTOs = session.getLines().stream()
                .map(sessionMapper::toLineDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lineDTOs);
    }
}
