package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.SubRecipeItem;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.repository.SubRecipeItemRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureRepository;
import com.rayvision.inventory_management.service.SubRecipeItemService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubRecipeItemServiceImpl implements SubRecipeItemService {
    private final SubRecipeItemRepository subRecipeItemRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public SubRecipeItemServiceImpl(SubRecipeItemRepository subRecipeItemRepository,
                                    SubRecipeRepository subRecipeRepository,
                                    UnitOfMeasureRepository unitOfMeasureRepository) {
        this.subRecipeItemRepository = subRecipeItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    @Override
    public List<SubRecipeItem> getItemsBySubRecipe(Long subRecipeId) {
        // If you want a custom query: findBySubRecipeId(subRecipeId)
        // If not, do:
        List<SubRecipeItem> all = subRecipeItemRepository.findAll();
        return all.stream()
                .filter(item -> item.getSubRecipe().getId().equals(subRecipeId))
                .toList();
    }

    @Override
    public Optional<SubRecipeItem> getOne(Long subRecipeId, Long itemId) {
        return subRecipeItemRepository.findById(itemId)
                .filter(item -> item.getSubRecipe().getId().equals(subRecipeId));
    }

    @Override
    public SubRecipeItem createItem(Long subRecipeId, SubRecipeItem item) {
        // fetch subRecipe
        SubRecipe parent = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));

        // link
        item.setSubRecipe(parent);

        // If the user provided a custom UOM or some logic, fetch from DB
        if (item.getUnitOfMeasure() != null && item.getUnitOfMeasure().getId() != null) {
            UnitOfMeasure existingUom = unitOfMeasureRepository.findById(item.getUnitOfMeasure().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found for: " + item.getUnitOfMeasure().getId()));
            item.setUnitOfMeasure(existingUom);
        }
        // if user provided inventoryItem, do something similar or trust item.getInventoryItem() ?

        return subRecipeItemRepository.save(item);
    }

    @Override
    public SubRecipeItem updateItem(Long subRecipeId, SubRecipeItem item) {
        SubRecipeItem existing = subRecipeItemRepository.findById(item.getId())
                .filter(i -> i.getSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException("Item not found or not in subRecipe " + subRecipeId));

        existing.setQuantity(item.getQuantity());
        existing.setWastagePercent(item.getWastagePercent());
        if (item.getUnitOfMeasure() != null) {
            // fetch from DB if needed
            existing.setUnitOfMeasure(item.getUnitOfMeasure());
        }
        if (item.getInventoryItem() != null) {
            existing.setInventoryItem(item.getInventoryItem());
        }
        return subRecipeItemRepository.save(existing);
    }

    @Override
    public SubRecipeItem partialUpdateItem(Long subRecipeId, SubRecipeItem item) {
        SubRecipeItem existing = subRecipeItemRepository.findById(item.getId())
                .filter(i -> i.getSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException("Item not found or not in subRecipe " + subRecipeId));

        if (item.getQuantity() != null) existing.setQuantity(item.getQuantity());
        if (item.getWastagePercent() != null) existing.setWastagePercent(item.getWastagePercent());
        if (item.getUnitOfMeasure() != null) {
            existing.setUnitOfMeasure(item.getUnitOfMeasure());
        }
        if (item.getInventoryItem() != null) {
            existing.setInventoryItem(item.getInventoryItem());
        }
        return subRecipeItemRepository.save(existing);
    }

    @Override
    public void deleteItem(Long subRecipeId, Long itemId) {
        SubRecipeItem existing = subRecipeItemRepository.findById(itemId)
                .filter(i -> i.getSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException("Item not found or not in subRecipe " + subRecipeId));
        subRecipeItemRepository.delete(existing);
    }

}
