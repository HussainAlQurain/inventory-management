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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubRecipeLineServiceImpl implements SubRecipeLineService {
    private final SubRecipeLineRepository subRecipeLineRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public SubRecipeLineServiceImpl(SubRecipeLineRepository subRecipeLineRepository,
                                    SubRecipeRepository subRecipeRepository,
                                    InventoryItemRepository inventoryItemRepository,
                                    UnitOfMeasureRepository unitOfMeasureRepository) {
        this.subRecipeLineRepository = subRecipeLineRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
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

        return subRecipeLineRepository.save(line);
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

        return subRecipeLineRepository.save(existing);
    }

    @Override
    public SubRecipeLine partialUpdateLine(Long subRecipeId, SubRecipeLine line) {
        SubRecipeLine existing = subRecipeLineRepository.findById(line.getId())
                .filter(l -> l.getParentSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException(
                        "Line not found or not in subRecipe: " + subRecipeId));

        // Only update fields if not null
        if (line.getQuantity() != null) {
            existing.setQuantity(line.getQuantity());
        }
        if (line.getWastagePercent() != null) {
            existing.setWastagePercent(line.getWastagePercent());
        }
        if (line.getInventoryItem() != null && line.getInventoryItem().getId() != null) {
            InventoryItem inv = inventoryItemRepository.findById(line.getInventoryItem().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem not found: " + line.getInventoryItem().getId()));
            existing.setInventoryItem(inv);
            existing.setChildSubRecipe(null);
        }
        if (line.getChildSubRecipe() != null && line.getChildSubRecipe().getId() != null) {
            SubRecipe child = subRecipeRepository.findById(line.getChildSubRecipe().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Child SubRecipe not found: " + line.getChildSubRecipe().getId()));
            existing.setChildSubRecipe(child);
            existing.setInventoryItem(null);
        }
        if (line.getUnitOfMeasure() != null && line.getUnitOfMeasure().getId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(line.getUnitOfMeasure().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "UOM not found: " + line.getUnitOfMeasure().getId()));
            existing.setUnitOfMeasure(uom);
        }

        return subRecipeLineRepository.save(existing);
    }

    @Override
    public void deleteLine(Long subRecipeId, Long lineId) {
        SubRecipeLine existing = subRecipeLineRepository.findById(lineId)
                .filter(l -> l.getParentSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException(
                        "Line not found or not in subRecipe: " + subRecipeId));
        subRecipeLineRepository.delete(existing);
    }

}
