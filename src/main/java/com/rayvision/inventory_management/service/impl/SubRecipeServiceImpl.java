package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.exceptions.ResourceNotFoundException;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeItemLineDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeLineDTO;
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
        // 1) Validate the company, build the SubRecipe
        // (same as you are doing now...)
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        SubRecipe subRecipe = new SubRecipe();
        subRecipe.setCompany(company);
        subRecipe.setName(dto.getName());
        subRecipe.setType(dto.getType());
        subRecipe.setYieldQty(dto.getYieldQty());
        subRecipe.setPhotoUrl(dto.getPhotoUrl());
        subRecipe.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        subRecipe.setCookTimeMinutes(dto.getCookTimeMinutes());
        subRecipe.setInstructions(dto.getInstructions());
        subRecipe.setCost(dto.getCost());

        // Category, UOM checks, etc.
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            subRecipe.setCategory(cat);
        } else {
            throw new RuntimeException("Category is required for SubRecipe");
        }

        if (dto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(dto.getUomId())
                    .orElseThrow(() -> new RuntimeException("UOM not found: " + dto.getUomId()));
            subRecipe.setUom(uom);
        } else {
            throw new RuntimeException("You must provide a UOM for subRecipe yield");
        }

        // 2) Build the SubRecipeLine set
        Set<SubRecipeLine> lineSet = new HashSet<>();
        if (dto.getLines() != null) {
            for (SubRecipeLineDTO lineDto : dto.getLines()) {
                SubRecipeLine line = new SubRecipeLine();
                line.setParentSubRecipe(subRecipe);

                // If referencing a raw InventoryItem:
                if (lineDto.getInventoryItemId() != null) {
                    InventoryItem invItem = inventoryItemRepository.findById(lineDto.getInventoryItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "InventoryItem not found: " + lineDto.getInventoryItemId()));
                    line.setInventoryItem(invItem);
                }

                // If referencing a child SubRecipe:
                if (lineDto.getChildSubRecipeId() != null) {
                    SubRecipe child = subRecipeRepository.findById(lineDto.getChildSubRecipeId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Child SubRecipe not found: " + lineDto.getChildSubRecipeId()));
                    line.setChildSubRecipe(child);
                }

                // quantity, wastage, unitOfMeasure
                line.setQuantity(lineDto.getQuantity() != null ? lineDto.getQuantity() : 0.0);
                line.setWastagePercent(
                        lineDto.getWastagePercent() != null ? lineDto.getWastagePercent() : 0.0
                );

                if (lineDto.getUnitOfMeasureId() != null) {
                    UnitOfMeasure lineUom = unitOfMeasureRepository.findById(lineDto.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + lineDto.getUnitOfMeasureId()));
                    line.setUnitOfMeasure(lineUom);
                } else {
                    // fallback logic if you want
                    // (e.g. require the user to supply a line UOM or do something else)
                    throw new RuntimeException("Must provide a UOM for each line");
                }

                lineSet.add(line);
            }
        }

        // 3) Attach lines to the subRecipe
        subRecipe.setSubRecipeLines(lineSet);

        // 4) Save the SubRecipe and recalc cost
        SubRecipe saved = subRecipeRepository.save(subRecipe);
        saved = recalcSubRecipeCost(saved);
        return subRecipeRepository.save(saved);
    }


    @Override
    @Transactional
    public SubRecipe updateSubRecipe(Long companyId, Long subRecipeId, SubRecipeCreateDTO dto) {
        // 1) Find existing
        SubRecipe existing = subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("SubRecipe", "id", subRecipeId));

        // 2) Update main fields
        existing.setName(dto.getName());
        existing.setType(dto.getType());
        existing.setYieldQty(dto.getYieldQty());
        existing.setPhotoUrl(dto.getPhotoUrl());
        existing.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        existing.setCookTimeMinutes(dto.getCookTimeMinutes());
        existing.setInstructions(dto.getInstructions());
        existing.setCost(dto.getCost()); // or compute after

        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            existing.setCategory(cat);
        }
        if (dto.getUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findById(dto.getUomId())
                    .orElseThrow(() -> new RuntimeException("UOM not found: " + dto.getUomId()));
            existing.setUom(uom);
        }

        // 3) Rebuild bridging lines if user provided new lines
        if (dto.getLines() != null) {
            // Clear out the old lines
            existing.getSubRecipeLines().clear();

            // Add the new ones
            for (SubRecipeLineDTO lineDto : dto.getLines()) {
                SubRecipeLine line = new SubRecipeLine();
                line.setParentSubRecipe(existing);

                if (lineDto.getInventoryItemId() != null) {
                    InventoryItem invItem = inventoryItemRepository.findById(lineDto.getInventoryItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "InventoryItem not found: " + lineDto.getInventoryItemId()));
                    line.setInventoryItem(invItem);
                }

                if (lineDto.getChildSubRecipeId() != null) {
                    SubRecipe child = subRecipeRepository.findById(lineDto.getChildSubRecipeId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Child SubRecipe not found: " + lineDto.getChildSubRecipeId()));
                    line.setChildSubRecipe(child);
                }

                line.setQuantity(lineDto.getQuantity() != null ? lineDto.getQuantity() : 0.0);
                line.setWastagePercent(
                        lineDto.getWastagePercent() != null ? lineDto.getWastagePercent() : 0.0
                );

                if (lineDto.getUnitOfMeasureId() != null) {
                    UnitOfMeasure lineUom = unitOfMeasureRepository.findById(lineDto.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + lineDto.getUnitOfMeasureId()));
                    line.setUnitOfMeasure(lineUom);
                } else {
                    throw new RuntimeException("Must provide a UOM for each line");
                }

                existing.getSubRecipeLines().add(line);
            }
        }

        // 4) Recompute cost
        existing = recalcSubRecipeCost(existing);

        // 5) Save
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
                    .orElseThrow(() -> new RuntimeException("UOM not found: " + dto.getUomId()));
            existing.setUom(uom);
        }

        // If we see dto.getLines() != null, decide how you want to handle it:
        // a) replace lines entirely
        // b) or “merge” them in some partial way, etc.
        if (dto.getLines() != null) {
            // example: full replace
            existing.getSubRecipeLines().clear();

            for (SubRecipeLineDTO lineDto : dto.getLines()) {
                SubRecipeLine line = new SubRecipeLine();
                line.setParentSubRecipe(existing);

                if (lineDto.getInventoryItemId() != null) {
                    InventoryItem invItem = inventoryItemRepository.findById(lineDto.getInventoryItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "InventoryItem not found: " + lineDto.getInventoryItemId()));
                    line.setInventoryItem(invItem);
                }

                if (lineDto.getChildSubRecipeId() != null) {
                    SubRecipe child = subRecipeRepository.findById(lineDto.getChildSubRecipeId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Child SubRecipe not found: " + lineDto.getChildSubRecipeId()));
                    line.setChildSubRecipe(child);
                }

                line.setQuantity(lineDto.getQuantity() != null ? lineDto.getQuantity() : 0.0);
                line.setWastagePercent(lineDto.getWastagePercent() != null ? lineDto.getWastagePercent() : 0.0);

                if (lineDto.getUnitOfMeasureId() != null) {
                    UnitOfMeasure lineUom = unitOfMeasureRepository.findById(lineDto.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + lineDto.getUnitOfMeasureId()));
                    line.setUnitOfMeasure(lineUom);
                } else {
                    throw new RuntimeException("Must provide a UOM for each line");
                }

                existing.getSubRecipeLines().add(line);
            }
        }

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
        double totalCost = 0.0;

        if (subRecipe.getSubRecipeLines() != null) {
            for (SubRecipeLine line : subRecipe.getSubRecipeLines()) {

                double lineCost = 0.0;
                double netQty = line.getQuantity() != null ? line.getQuantity() : 0.0;
                double wastage = line.getWastagePercent() != null ? (line.getWastagePercent() / 100) : 0.0;
                double grossQty = netQty * (1.0 + wastage);

                if (line.getInventoryItem() != null) {
                    // This line references a raw InventoryItem
                    double itemPrice = Optional.ofNullable(line.getInventoryItem().getCurrentPrice()).orElse(0.0);
                    lineCost = itemPrice * grossQty;
                }
                else if (line.getChildSubRecipe() != null) {
                    // This line references another (child) SubRecipe
                    SubRecipe child = line.getChildSubRecipe();
                    // Ensure child’s cost is up-to-date (depending on your logic)
                    recalcSubRecipeCost(child);

                    double childCost = Optional.ofNullable(child.getCost()).orElse(0.0);
                    double childYield = Optional.ofNullable(child.getYieldQty()).orElse(1.0);

                    if (childYield > 0.0) {
                        // portion ratio * child's total cost
                        lineCost = (grossQty / childYield) * childCost;
                    } else {
                        // fallback
                        lineCost = childCost;
                    }
                }

                line.setLineCost(lineCost); // if you want to store it per line
                totalCost += lineCost;
            }
        }

        subRecipe.setCost(totalCost);
        return subRecipe;
    }

    @Override
    public List<SubRecipe> searchSubRecipes(Long companyId, String searchTerm) {
        return subRecipeRepository.searchSubRecipes(companyId, searchTerm);
    }

}
