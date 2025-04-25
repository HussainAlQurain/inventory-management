package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.SubRecipeLine;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.SubRecipeLineRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureRepository;
import com.rayvision.inventory_management.service.SubRecipeLineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubRecipeLineServiceImpl implements SubRecipeLineService {
    private final SubRecipeLineRepository subRecipeLineRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final SubRecipeCostCalculationService subRecipeCostCalculationService;

    public SubRecipeLineServiceImpl(SubRecipeLineRepository subRecipeLineRepository,
                                    SubRecipeRepository subRecipeRepository,
                                    InventoryItemRepository inventoryItemRepository,
                                    UnitOfMeasureRepository unitOfMeasureRepository,
                                    SubRecipeCostCalculationService subRecipeCostCalculationService) {
        this.subRecipeLineRepository = subRecipeLineRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.subRecipeCostCalculationService = subRecipeCostCalculationService;
    }

    @Override
    public List<SubRecipeLine> getLinesBySubRecipe(Long subRecipeId) {
        // If you have a custom query like: List<SubRecipeLine> findByParentSubRecipeId(Long subRecipeId)
        // then just do: return subRecipeLineRepository.findByParentSubRecipeId(subRecipeId);
        // Otherwise, do a simple filtering in memory:
        List<SubRecipeLine> all = subRecipeLineRepository.findAll();
        return all.stream()
                .filter(line -> line.getParentSubRecipe().getId().equals(subRecipeId))
                .toList();
    }

    @Override
    public Page<SubRecipeLine> getLinesBySubRecipe(Long subRecipeId, String search, Pageable pageable) {
        // Get all lines for the sub recipe
        List<SubRecipeLine> allLines = subRecipeLineRepository.findAll().stream()
                .filter(line -> line.getParentSubRecipe().getId().equals(subRecipeId))
                .collect(Collectors.toList());
        
        // Apply search filter if provided
        if (StringUtils.hasText(search)) {
            String searchLower = search.toLowerCase();
            allLines = allLines.stream()
                    .filter(line -> {
                        // Search in inventory item name if exists
                        boolean matchesInventoryItem = line.getInventoryItem() != null && 
                                line.getInventoryItem().getName() != null &&
                                line.getInventoryItem().getName().toLowerCase().contains(searchLower);
                        
                        // Search in child sub recipe name if exists
                        boolean matchesChildSubRecipe = line.getChildSubRecipe() != null &&
                                line.getChildSubRecipe().getName() != null &&
                                line.getChildSubRecipe().getName().toLowerCase().contains(searchLower);
                        
                        return matchesInventoryItem || matchesChildSubRecipe;
                    })
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allLines.size());
        
        // Handle case when start index is out of bounds
        if (start >= allLines.size()) {
            return new PageImpl<>(List.of(), pageable, allLines.size());
        }
        
        List<SubRecipeLine> pageContent = allLines.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allLines.size());
    }

    @Override
    public Optional<SubRecipeLine> getOne(Long subRecipeId, Long lineId) {
        return subRecipeLineRepository.findById(lineId)
                .filter(line -> line.getParentSubRecipe().getId().equals(subRecipeId));
    }

    @Override
    public SubRecipeLine createLine(Long subRecipeId, SubRecipeLine line) {
        // 1) Ensure parent SubRecipe exists
        SubRecipe parent = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));
        // 2) Link
        line.setParentSubRecipe(parent);

        // 3) If referencing an inventory item, ensure it exists
        if (line.getInventoryItem() != null && line.getInventoryItem().getId() != null) {
            InventoryItem inv = inventoryItemRepository.findById(line.getInventoryItem().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem not found: " + line.getInventoryItem().getId()));
            line.setInventoryItem(inv);
        }
        // 4) If referencing a child SubRecipe, ensure it exists
        if (line.getChildSubRecipe() != null && line.getChildSubRecipe().getId() != null) {
            SubRecipe child = subRecipeRepository.findById(line.getChildSubRecipe().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Child SubRecipe not found: " + line.getChildSubRecipe().getId()));
            line.setChildSubRecipe(child);
        }

        // 5) If referencing a UOM, ensure it exists
        if (line.getUnitOfMeasure() != null && line.getUnitOfMeasure().getId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(line.getUnitOfMeasure().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "UOM not found: " + line.getUnitOfMeasure().getId()));
            line.setUnitOfMeasure(uom);
        }
        // Calculate and set line cost
        line.setLineCost(subRecipeCostCalculationService.calculateLineCost(line));
        SubRecipeLine savedLine = subRecipeLineRepository.save(line);

        // Recalculate parent's total cost
        subRecipeCostCalculationService.recalculateSubRecipeCost(parent);
        subRecipeRepository.save(parent);

        return savedLine;

    }

    @Override
    public SubRecipeLine updateLine(Long subRecipeId, SubRecipeLine line) {
        // 1) Find existing line
        SubRecipeLine existing = subRecipeLineRepository.findById(line.getId())
                .filter(l -> l.getParentSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException(
                        "Line not found or not in subRecipe: " + subRecipeId));

        // 2) Overwrite
        existing.setQuantity(line.getQuantity());
        existing.setWastagePercent(line.getWastagePercent());

        // If referencing new inventory item
        if (line.getInventoryItem() != null && line.getInventoryItem().getId() != null) {
            InventoryItem inv = inventoryItemRepository.findById(line.getInventoryItem().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem not found: " + line.getInventoryItem().getId()));
            existing.setInventoryItem(inv);
            // Also clear out childSubRecipe if needed or keep both if you allow that
            existing.setChildSubRecipe(null);
        }

        // If referencing new child subRecipe
        if (line.getChildSubRecipe() != null && line.getChildSubRecipe().getId() != null) {
            SubRecipe child = subRecipeRepository.findById(line.getChildSubRecipe().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Child SubRecipe not found: " + line.getChildSubRecipe().getId()));
            existing.setChildSubRecipe(child);
            // Possibly clear out inventoryItem if you don't allow both
            existing.setInventoryItem(null);
        }

        // If referencing a new UOM
        if (line.getUnitOfMeasure() != null && line.getUnitOfMeasure().getId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(line.getUnitOfMeasure().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "UOM not found: " + line.getUnitOfMeasure().getId()));
            existing.setUnitOfMeasure(uom);
        }

        existing.setLineCost(subRecipeCostCalculationService.calculateLineCost(existing));
        SubRecipeLine updatedLine = subRecipeLineRepository.save(existing);

        // Recalculate parent's total cost
        SubRecipe parent = subRecipeRepository.findById(subRecipeId).orElseThrow();
        subRecipeCostCalculationService.recalculateSubRecipeCost(parent);
        subRecipeRepository.save(parent);

        return updatedLine;
    }

    @Override
    public SubRecipeLine partialUpdateLine(Long subRecipeId, SubRecipeLine line) {
        SubRecipeLine existing = subRecipeLineRepository.findById(line.getId())
                .filter(l -> l.getParentSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException(
                        "Line not found or not in subRecipe: " + subRecipeId));

        // Track if any cost-affecting field changes
        boolean costAffectingChange = false;

        // Only update fields if not null
        if (line.getQuantity() != null) {
            existing.setQuantity(line.getQuantity());
            costAffectingChange = true;
        }
        if (line.getWastagePercent() != null) {
            existing.setWastagePercent(line.getWastagePercent());
            costAffectingChange = true;
        }
        if (line.getInventoryItem() != null && line.getInventoryItem().getId() != null) {
            InventoryItem inv = inventoryItemRepository.findById(line.getInventoryItem().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem not found: " + line.getInventoryItem().getId()));
            existing.setInventoryItem(inv);
            existing.setChildSubRecipe(null);
            costAffectingChange = true;
        }
        if (line.getChildSubRecipe() != null && line.getChildSubRecipe().getId() != null) {
            SubRecipe child = subRecipeRepository.findById(line.getChildSubRecipe().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Child SubRecipe not found: " + line.getChildSubRecipe().getId()));
            existing.setChildSubRecipe(child);
            existing.setInventoryItem(null);
            costAffectingChange = true;
        }
        if (line.getUnitOfMeasure() != null && line.getUnitOfMeasure().getId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(line.getUnitOfMeasure().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "UOM not found: " + line.getUnitOfMeasure().getId()));
            existing.setUnitOfMeasure(uom);
            costAffectingChange = true;
        }

        // Only recalculate if relevant fields changed
        if (costAffectingChange) {
            existing.setLineCost(subRecipeCostCalculationService.calculateLineCost(existing));
        }

        SubRecipeLine updatedLine = subRecipeLineRepository.save(existing);

        // Always recalculate parent cost (even if line cost didn't change, other lines might have)
        SubRecipe parent = subRecipeRepository.findById(subRecipeId).orElseThrow();
        subRecipeCostCalculationService.recalculateSubRecipeCost(parent);
        subRecipeRepository.save(parent);

        return updatedLine;

    }

    @Override
    public void deleteLine(Long subRecipeId, Long lineId) {
        SubRecipeLine existing = subRecipeLineRepository.findById(lineId)
                .filter(l -> l.getParentSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException(
                        "Line not found or not in subRecipe: " + subRecipeId));
        subRecipeLineRepository.delete(existing);

        SubRecipe parent = subRecipeRepository.findById(subRecipeId).orElseThrow();
        subRecipeCostCalculationService.recalculateSubRecipeCost(parent);
        subRecipeRepository.save(parent);
    }

}
