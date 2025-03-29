package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;
import com.rayvision.inventory_management.model.dto.MenuItemInventoryLineDTO;
import com.rayvision.inventory_management.model.dto.MenuItemSubRecipeLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.MenuItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository,
                               CompanyRepository companyRepository,
                               CategoryRepository categoryRepository,
                               InventoryItemRepository inventoryItemRepository,
                               SubRecipeRepository subRecipeRepository,
                               UnitOfMeasureRepository unitOfMeasureRepository) {
        this.menuItemRepository = menuItemRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    @Override
    public List<MenuItem> getAllMenuItems(Long companyId) {
        // If you want a findByCompanyId in the repo, you can do that. Or filter in memory:
        List<MenuItem> all = menuItemRepository.findAll();
        return all.stream()
                .filter(mi -> mi.getCompany().getId().equals(companyId))
                .toList();
    }

    @Override
    public Optional<MenuItem> getMenuItemById(Long companyId, Long menuItemId) {
        return menuItemRepository.findById(menuItemId)
                .filter(mi -> mi.getCompany().getId().equals(companyId));
    }

    @Override
    @Transactional
    public MenuItem createMenuItem(Long companyId, MenuItemCreateDTO dto) {
        // 1) Validate company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        // 2) Build new MenuItem
        MenuItem menuItem = new MenuItem();
        menuItem.setCompany(company);
        menuItem.setName(dto.getName());
        menuItem.setPosCode(dto.getPosCode());
        menuItem.setRetailPriceExclTax(dto.getRetailPriceExclTax());
        menuItem.setMaxAllowedFoodCostPct(dto.getMaxAllowedFoodCostPct());
        menuItem.setModifierGroups(dto.getModifierGroups());

        // If you pass cost/foodCost in the DTO, you can store them or compute
        // but typically we compute from bridging lines:
        menuItem.setCost(dto.getCost()); // optional
        menuItem.setFoodCostPercentage(dto.getFoodCostPercentage()); // optional

        // 3) Handle category
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            menuItem.setCategory(cat);
        } else {
            throw new RuntimeException("CategoryId is required for MenuItem");
        }

        // 4) Prepare bridging sets
        Set<MenuItemInventoryItem> invSet = new HashSet<>();
        if (dto.getInventoryLines() != null) {
            for (MenuItemInventoryLineDTO lineDTO : dto.getInventoryLines()) {
                // build bridging entity
                MenuItemInventoryItem bridging = new MenuItemInventoryItem();
                bridging.setMenuItem(menuItem);

                // fetch inventory item
                InventoryItem invItem = inventoryItemRepository.findById(lineDTO.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException(
                                "InventoryItem not found: " + lineDTO.getInventoryItemId()));
                bridging.setInventoryItem(invItem);

                // handle UOM
                if (lineDTO.getUnitOfMeasureId() != null) {
                    UnitOfMeasure uom = unitOfMeasureRepository.findById(lineDTO.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + lineDTO.getUnitOfMeasureId()));
                    bridging.setUnitOfMeasure(uom);
                } else {
                    // default to the inventoryItem’s UOM or force user to specify
                    bridging.setUnitOfMeasure(invItem.getInventoryUom());
                }

                bridging.setQuantity(lineDTO.getQuantity());
                bridging.setWastagePercent(lineDTO.getWastagePercent());
                // bridging.setCost(...) if you want to store line cost

                invSet.add(bridging);
            }
        }

        Set<MenuItemSubRecipe> subSet = new HashSet<>();
        if (dto.getSubRecipeLines() != null) {
            for (MenuItemSubRecipeLineDTO lineDTO : dto.getSubRecipeLines()) {
                MenuItemSubRecipe bridging = new MenuItemSubRecipe();
                bridging.setMenuItem(menuItem);

                SubRecipe subRec = subRecipeRepository.findById(lineDTO.getSubRecipeId())
                        .orElseThrow(() -> new RuntimeException(
                                "SubRecipe not found: " + lineDTO.getSubRecipeId()));
                bridging.setSubRecipe(subRec);

                // handle UOM
                if (lineDTO.getUnitOfMeasureId() != null) {
                    UnitOfMeasure uom = unitOfMeasureRepository.findById(lineDTO.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + lineDTO.getUnitOfMeasureId()));
                    bridging.setUnitOfMeasure(uom);
                } else {
                    bridging.setUnitOfMeasure(subRec.getUom());
                }

                bridging.setQuantity(lineDTO.getQuantity());
                bridging.setWastagePercent(lineDTO.getWastagePercent());
                subSet.add(bridging);
            }
        }

        menuItem.setMenuItemInventoryItems(invSet);
        menuItem.setMenuItemSubRecipes(subSet);

        // 5) Save the new menuItem (cascade bridging lines if cascade=ALL)
        MenuItem saved = menuItemRepository.save(menuItem);

        // 6) Optionally compute cost now
        saved = recalcMenuItemCost(saved);

        return menuItemRepository.save(saved);  // save again if cost changed
    }

    @Override
    @Transactional
    public MenuItem updateMenuItem(Long companyId, Long menuItemId, MenuItemCreateDTO dto) {
        MenuItem existing = menuItemRepository.findById(menuItemId)
                .filter(mi -> mi.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "MenuItem not found or not in company: " + menuItemId));

        // Overwrite all main fields
        existing.setName(dto.getName());
        existing.setPosCode(dto.getPosCode());
        existing.setRetailPriceExclTax(dto.getRetailPriceExclTax());
        existing.setMaxAllowedFoodCostPct(dto.getMaxAllowedFoodCostPct());
        existing.setModifierGroups(dto.getModifierGroups());
        existing.setCost(dto.getCost());
        existing.setFoodCostPercentage(dto.getFoodCostPercentage());

        // Possibly handle category
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            existing.setCategory(cat);
        }

        // Overwrite bridging lines if you want to do a “full replace” approach:
        if (dto.getInventoryLines() != null) {
            // clear existing bridging
            existing.getMenuItemInventoryItems().clear();
            // rebuild bridging
            for (MenuItemInventoryLineDTO lineDTO : dto.getInventoryLines()) {
                MenuItemInventoryItem bridging = new MenuItemInventoryItem();
                bridging.setMenuItem(existing);

                InventoryItem invItem = inventoryItemRepository.findById(lineDTO.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException("InventoryItem not found: " + lineDTO.getInventoryItemId()));
                bridging.setInventoryItem(invItem);

                if (lineDTO.getUnitOfMeasureId() != null) {
                    UnitOfMeasure uom = unitOfMeasureRepository.findById(lineDTO.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException("UOM not found: " + lineDTO.getUnitOfMeasureId()));
                    bridging.setUnitOfMeasure(uom);
                } else {
                    bridging.setUnitOfMeasure(invItem.getInventoryUom());
                }
                bridging.setQuantity(lineDTO.getQuantity());
                bridging.setWastagePercent(lineDTO.getWastagePercent());

                existing.getMenuItemInventoryItems().add(bridging);
            }
        }

        if (dto.getSubRecipeLines() != null) {
            // clear existing bridging
            existing.getMenuItemSubRecipes().clear();
            for (MenuItemSubRecipeLineDTO lineDTO : dto.getSubRecipeLines()) {
                MenuItemSubRecipe bridging = new MenuItemSubRecipe();
                bridging.setMenuItem(existing);

                SubRecipe subRec = subRecipeRepository.findById(lineDTO.getSubRecipeId())
                        .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + lineDTO.getSubRecipeId()));
                bridging.setSubRecipe(subRec);

                if (lineDTO.getUnitOfMeasureId() != null) {
                    UnitOfMeasure uom = unitOfMeasureRepository.findById(lineDTO.getUnitOfMeasureId())
                            .orElseThrow(() -> new RuntimeException("UOM not found: " + lineDTO.getUnitOfMeasureId()));
                    bridging.setUnitOfMeasure(uom);
                } else {
                    bridging.setUnitOfMeasure(subRec.getUom());
                }
                bridging.setQuantity(lineDTO.getQuantity());
                bridging.setWastagePercent(lineDTO.getWastagePercent());

                existing.getMenuItemSubRecipes().add(bridging);
            }
        }

        // Recompute cost
        existing = recalcMenuItemCost(existing);

        return menuItemRepository.save(existing);
    }

    @Override
    @Transactional
    public MenuItem partialUpdateMenuItem(Long companyId, Long menuItemId, MenuItemCreateDTO dto) {
        MenuItem existing = menuItemRepository.findById(menuItemId)
                .filter(mi -> mi.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "MenuItem not found or not in company: " + menuItemId));

        // Only update if non-null
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getPosCode() != null) existing.setPosCode(dto.getPosCode());
        if (dto.getRetailPriceExclTax() != null) existing.setRetailPriceExclTax(dto.getRetailPriceExclTax());
        if (dto.getMaxAllowedFoodCostPct() != null) existing.setMaxAllowedFoodCostPct(dto.getMaxAllowedFoodCostPct());
        if (dto.getModifierGroups() != null) existing.setModifierGroups(dto.getModifierGroups());
        if (dto.getCost() != null) existing.setCost(dto.getCost());
        if (dto.getFoodCostPercentage() != null) existing.setFoodCostPercentage(dto.getFoodCostPercentage());

        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            existing.setCategory(cat);
        }

        // If bridging lines are not null, we might interpret that as "update bridging lines"
        // or we can skip them if you want partial logic.
        if (dto.getInventoryLines() != null) {
            // handle partial bridging logic or full replacement, depending on your design
            // For example, you might skip or do a merge approach.
        }
        if (dto.getSubRecipeLines() != null) {
            // same approach
        }

        // Recompute cost (if bridging lines changed)
        existing = recalcMenuItemCost(existing);

        return menuItemRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteMenuItemById(Long companyId, Long menuItemId) {
        MenuItem existing = menuItemRepository.findById(menuItemId)
                .filter(mi -> mi.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "MenuItem not found or not in this company: " + menuItemId
                ));
        menuItemRepository.delete(existing);
    }

    @Override
    @Transactional
    public MenuItem recalcMenuItemCost(MenuItem menuItem) {
        // Recompute total cost from bridging lines
        double totalCost = 0.0;

        if (menuItem.getMenuItemInventoryItems() != null) {
            for (MenuItemInventoryItem line : menuItem.getMenuItemInventoryItems()) {
                // get the inventory item’s cost
                Double itemCost = Optional.ofNullable(line.getInventoryItem().getCurrentPrice()).orElse(0.0);
                // compute line usage
                double grossQty = line.getQuantity() * (1.0 + Optional.ofNullable(line.getWastagePercent()).orElse(0.0));
                double lineCost = itemCost * grossQty;
                // store or skip line.setCost(lineCost);
                totalCost += lineCost;
            }
        }

        if (menuItem.getMenuItemSubRecipes() != null) {
            for (MenuItemSubRecipe line : menuItem.getMenuItemSubRecipes()) {
                // subRecipe cost
                Double subRecCost = Optional.ofNullable(line.getSubRecipe().getCost()).orElse(0.0);
                // compute usage
                double grossQty = line.getQuantity() * (1.0 + Optional.ofNullable(line.getWastagePercent()).orElse(0.0));
                double lineCost = subRecCost * grossQty;
                // line.setCost(lineCost);
                totalCost += lineCost;
            }
        }

        menuItem.setCost(totalCost);

        // If retailPriceExclTax is not null
        if (menuItem.getRetailPriceExclTax() != null && menuItem.getRetailPriceExclTax() > 0.0) {
            double fc = (totalCost / menuItem.getRetailPriceExclTax()) * 100.0;
            menuItem.setFoodCostPercentage(fc);
        } else {
            // or set to null if retailPrice is 0
            menuItem.setFoodCostPercentage(null);
        }

        return menuItem;
    }

    @Override
    public List<MenuItem> searchMenuItems(Long companyId, String searchTerm) {
        return menuItemRepository.searchMenuItems(companyId, searchTerm);
    }

}
