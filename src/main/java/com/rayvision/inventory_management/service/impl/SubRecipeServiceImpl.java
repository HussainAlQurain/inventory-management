package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeItemLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.SubRecipeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SubRecipeServiceImpl implements SubRecipeService {

    private final SubRecipeRepository subRecipeRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SubRecipeItemRepository subRecipeItemRepository;

    public SubRecipeServiceImpl(SubRecipeRepository subRecipeRepository,
                                CompanyRepository companyRepository,
                                CategoryRepository categoryRepository,
                                UnitOfMeasureRepository unitOfMeasureRepository,
                                InventoryItemRepository inventoryItemRepository,
                                SubRecipeItemRepository subRecipeItemRepository) {
        this.subRecipeRepository = subRecipeRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeItemRepository = subRecipeItemRepository;
    }

    @Override
    public List<SubRecipe> getAllSubRecipes(Long companyId) {
        // If you have "findByCompanyId(...)":
        // return subRecipeRepository.findByCompanyId(companyId);
        List<SubRecipe> all = subRecipeRepository.findAll();
        return all.stream()
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .toList();
    }

    @Override
    public Optional<SubRecipe> getSubRecipeById(Long companyId, Long subRecipeId) {
        return subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId));
    }

    @Override
    @Transactional
    public SubRecipe createSubRecipe(Long companyId, SubRecipeCreateDTO dto) {
        // 1) Validate the company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found for id: " + companyId));

        // 2) Build new SubRecipe
        SubRecipe subRecipe = new SubRecipe();
        subRecipe.setCompany(company);
        subRecipe.setName(dto.getName());
        subRecipe.setType(dto.getType()); // PREPARATION or SUB_RECIPE
        subRecipe.setYieldQty(dto.getYieldQty());
        subRecipe.setPhotoUrl(dto.getPhotoUrl());
        subRecipe.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        subRecipe.setCookTimeMinutes(dto.getCookTimeMinutes());
        subRecipe.setInstructions(dto.getInstructions());
        subRecipe.setCost(dto.getCost());  // or compute later

        // 3) Handle category
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            subRecipe.setCategory(cat);
        } else {
            // Possibly require a category
            throw new RuntimeException("Category is required for SubRecipe");
        }

        // 4) UOM
        if (dto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(dto.getUomId())
                    .orElseThrow(() -> new RuntimeException("UOM not found for id: " + dto.getUomId()));
            subRecipe.setUom(uom);
        } else {
            throw new RuntimeException("You must provide a UOM for subRecipe yield");
        }

        // 5) Build bridging lines (SubRecipeItems)
        //    We'll hold them in a set
        Set<SubRecipeItem> itemSet = new HashSet<>();
        if (dto.getIngredients() != null) {
            for (SubRecipeItemLineDTO line : dto.getIngredients()) {
                SubRecipeItem bridging = new SubRecipeItem();
                bridging.setSubRecipe(subRecipe);

                // find the InventoryItem
                InventoryItem invItem = inventoryItemRepository.findById(line.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException(
                                "InventoryItem not found: " + line.getInventoryItemId()));

                bridging.setInventoryItem(invItem);
                bridging.setQuantity(line.getQuantity());

                // If user specified a UOM for this line
                if (line.getUnitOfMeasureId() != null) {
                    UnitOfMeasure lineUom = unitOfMeasureRepository.findById(line.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + line.getUnitOfMeasureId()));
                    bridging.setUnitOfMeasure(lineUom);
                } else {
                    // default to the inventoryItem’s own UOM or require a line UOM
                    bridging.setUnitOfMeasure(invItem.getInventoryUom());
                }

                // If you want to store wastage:
                // bridging.setWastagePercent(line.getWastagePercent()) => you'd add that field to SubRecipeItem

                itemSet.add(bridging);
            }
        }

        // 6) Attach the bridging lines to subRecipe
        subRecipe.setSubRecipeItems(itemSet);

        // 7) Save initial subRecipe
        SubRecipe saved = subRecipeRepository.save(subRecipe);

        // 8) Recompute cost if you want to do so
        saved = recalcSubRecipeCost(saved);

        // 9) Save again if cost changed
        saved = subRecipeRepository.save(saved);

        return saved;
    }

    @Override
    @Transactional
    public SubRecipe updateSubRecipe(Long companyId, Long subRecipeId, SubRecipeCreateDTO dto) {
        // 1) Find existing
        SubRecipe existing = subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "SubRecipe not found or not in this company: " + subRecipeId));

        // 2) Overwrite main fields
        existing.setName(dto.getName());
        existing.setType(dto.getType());
        existing.setYieldQty(dto.getYieldQty());
        existing.setPhotoUrl(dto.getPhotoUrl());
        existing.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        existing.setCookTimeMinutes(dto.getCookTimeMinutes());
        existing.setInstructions(dto.getInstructions());
        existing.setCost(dto.getCost()); // or we will compute

        // 3) Category
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                            "Category not found: " + dto.getCategoryId()));
            existing.setCategory(cat);
        }

        // 4) UOM
        if (dto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(dto.getUomId())
                    .orElseThrow(() -> new RuntimeException(
                            "UOM not found: " + dto.getUomId()));
            existing.setUom(uom);
        }

        // 5) Rebuild bridging lines if user wants a “full replace”
        if (dto.getIngredients() != null) {
            existing.getSubRecipeItems().clear();
            for (SubRecipeItemLineDTO line : dto.getIngredients()) {
                SubRecipeItem bridging = new SubRecipeItem();
                bridging.setSubRecipe(existing);

                InventoryItem invItem = inventoryItemRepository.findById(line.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException(
                                "InventoryItem not found: " + line.getInventoryItemId()));
                bridging.setInventoryItem(invItem);
                bridging.setQuantity(line.getQuantity());

                if (line.getUnitOfMeasureId() != null) {
                    UnitOfMeasure lineUom = unitOfMeasureRepository.findById(line.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + line.getUnitOfMeasureId()));
                    bridging.setUnitOfMeasure(lineUom);
                } else {
                    bridging.setUnitOfMeasure(invItem.getInventoryUom());
                }

                existing.getSubRecipeItems().add(bridging);
            }
        }

        // 6) Recompute cost
        existing = recalcSubRecipeCost(existing);

        // 7) Save
        return subRecipeRepository.save(existing);
    }

    @Override
    @Transactional
    public SubRecipe partialUpdateSubRecipe(Long companyId, Long subRecipeId, SubRecipeCreateDTO dto) {
        SubRecipe existing = subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "SubRecipe not found or not in this company: " + subRecipeId));

        // Only update if not null
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getYieldQty() != null) existing.setYieldQty(dto.getYieldQty());
        if (dto.getPhotoUrl() != null) existing.setPhotoUrl(dto.getPhotoUrl());
        if (dto.getPrepTimeMinutes() != null) existing.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        if (dto.getCookTimeMinutes() != null) existing.setCookTimeMinutes(dto.getCookTimeMinutes());
        if (dto.getInstructions() != null) existing.setInstructions(dto.getInstructions());
        if (dto.getCost() != null) existing.setCost(dto.getCost());

        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                            "Category not found: " + dto.getCategoryId()));
            existing.setCategory(cat);
        }

        if (dto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(dto.getUomId())
                    .orElseThrow(() -> new RuntimeException(
                            "UOM not found: " + dto.getUomId()));
            existing.setUom(uom);
        }

        // If dto.getIngredients() != null, decide if that means "replace bridging lines"
        // or "do partial bridging."
        // We can skip bridging lines for partial update or handle merges.

        existing = recalcSubRecipeCost(existing);
        return subRecipeRepository.save(existing);
    }

    @Override
    public void deleteSubRecipeById(Long companyId, Long subRecipeId) {
        SubRecipe existing = subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "Cannot find subRecipe: " + subRecipeId + " or not in company " + companyId));
        subRecipeRepository.delete(existing);
    }

    @Override
    @Transactional
    public SubRecipe recalcSubRecipeCost(SubRecipe subRecipe) {
        double total = 0.0;

        if (subRecipe.getSubRecipeItems() != null) {
            for (SubRecipeItem line : subRecipe.getSubRecipeItems()) {
                // get inventory item’s current price
                double itemPrice = Optional.ofNullable(line.getInventoryItem().getCurrentPrice()).orElse(0.0);
                // possibly get wastage if you have a field in SubRecipeItem for it
                // double wastage = line.getWastagePercent() != null ? line.getWastagePercent() : 0.0;
                // double grossQty = line.getQuantity() * (1.0 + wastage);

                double grossQty = line.getQuantity(); // if you’re ignoring wastage for now
                double lineCost = itemPrice * grossQty;
                total += lineCost;
            }
        }

        subRecipe.setCost(total);
        return subRecipe;
    }

}
