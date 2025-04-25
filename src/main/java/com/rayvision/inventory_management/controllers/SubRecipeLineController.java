package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.SubRecipeLineMapper;
import com.rayvision.inventory_management.model.SubRecipeLine;
import com.rayvision.inventory_management.model.dto.SubRecipeLineDTO;
import com.rayvision.inventory_management.service.SubRecipeLineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sub-recipes/{subRecipeId}/lines")
public class SubRecipeLineController {
    private final SubRecipeLineService subRecipeLineService;
    private final SubRecipeLineMapper subRecipeLineMapper;

    public SubRecipeLineController(SubRecipeLineService subRecipeLineService,
                                   SubRecipeLineMapper subRecipeLineMapper) {
        this.subRecipeLineService = subRecipeLineService;
        this.subRecipeLineMapper = subRecipeLineMapper;
    }

    // GET all lines for a subRecipe
    @GetMapping
    public ResponseEntity<List<SubRecipeLineDTO>> getAllLines(@PathVariable Long subRecipeId) {
        List<SubRecipeLine> lines = subRecipeLineService.getLinesBySubRecipe(subRecipeId);
        List<SubRecipeLineDTO> dtos = lines.stream()
                .map(subRecipeLineMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // GET paginated lines
    @GetMapping("/paginated")
    public ResponseEntity<Page<SubRecipeLineDTO>> getPaginatedLines(
            @PathVariable Long subRecipeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SubRecipeLine> paginatedLines = subRecipeLineService.getLinesBySubRecipe(subRecipeId, search, pageable);
        Page<SubRecipeLineDTO> paginatedDtos = paginatedLines.map(subRecipeLineMapper::toDto);

        return ResponseEntity.ok(paginatedDtos);
    }

    // GET single line
    @GetMapping("/{lineId}")
    public ResponseEntity<SubRecipeLineDTO> getLine(@PathVariable Long subRecipeId,
                                                    @PathVariable Long lineId) {
        return subRecipeLineService.getOne(subRecipeId, lineId)
                .map(subRecipeLineMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE new line
    @PostMapping
    public ResponseEntity<SubRecipeLineDTO> createLine(@PathVariable Long subRecipeId,
                                                       @RequestBody SubRecipeLineDTO dto) {
        // Map DTO → Entity
        SubRecipeLine entity = subRecipeLineMapper.toEntity(dto);

        // Create
        SubRecipeLine created = subRecipeLineService.createLine(subRecipeId, entity);

        // Map back to DTO
        SubRecipeLineDTO response = subRecipeLineMapper.toDto(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // UPDATE (full) line
    @PutMapping("/{lineId}")
    public ResponseEntity<SubRecipeLineDTO> updateLine(@PathVariable Long subRecipeId,
                                                       @PathVariable Long lineId,
                                                       @RequestBody SubRecipeLineDTO dto) {
        dto.setId(lineId);

        SubRecipeLine entity = subRecipeLineMapper.toEntity(dto);
        try {
            SubRecipeLine updated = subRecipeLineService.updateLine(subRecipeId, entity);
            return ResponseEntity.ok(subRecipeLineMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PARTIAL update
    @PatchMapping("/{lineId}")
    public ResponseEntity<SubRecipeLineDTO> partialUpdateLine(@PathVariable Long subRecipeId,
                                                              @PathVariable Long lineId,
                                                              @RequestBody SubRecipeLineDTO dto) {
        dto.setId(lineId);

        SubRecipeLine entity = subRecipeLineMapper.toEntity(dto);
        try {
            SubRecipeLine updated = subRecipeLineService.partialUpdateLine(subRecipeId, entity);
            return ResponseEntity.ok(subRecipeLineMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE line
    @DeleteMapping("/{lineId}")
    public ResponseEntity<Void> deleteLine(@PathVariable Long subRecipeId,
                                           @PathVariable Long lineId) {
        try {
            subRecipeLineService.deleteLine(subRecipeId, lineId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
