package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;
import com.rayvision.inventory_management.model.dto.MenuItemInventoryLineDTO;
import com.rayvision.inventory_management.model.dto.MenuItemLineDTO;
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
    private final MenuItemLineRepository menuItemLineRepository; // Add this

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository,
                               CompanyRepository companyRepository,
                               CategoryRepository categoryRepository,
                               InventoryItemRepository inventoryItemRepository,
                               SubRecipeRepository subRecipeRepository,
                               UnitOfMeasureRepository unitOfMeasureRepository,
                               MenuItemLineRepository menuItemLineRepository) {
        this.menuItemRepository = menuItemRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.menuItemLineRepository = menuItemLineRepository;
    }

    @Override
    public List<MenuItem> getAllMenuItems(Long companyId) {
        return menuItemRepository.findByCompanyId(companyId);
    }

    @Override
    public Optional<MenuItem> getMenuItemById(Long companyId, Long menuItemId) {
        return menuItemRepository.findByIdAndCompanyId(menuItemId, companyId);
    }

    @Override
    @Transactional
    public MenuItem createMenuItem(Long companyId, MenuItemCreateDTO dto) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        MenuItem menuItem = new MenuItem();
        menuItem.setCompany(company);
        menuItem.setName(dto.getName());
        menuItem.setPosCode(dto.getPosCode());
        menuItem.setRetailPriceExclTax(dto.getRetailPriceExclTax());
        menuItem.setMaxAllowedFoodCostPct(dto.getMaxAllowedFoodCostPct());
        menuItem.setModifierGroups(dto.getModifierGroups());

        // Handle category
        Category category = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
        menuItem.setCategory(category);

        // Handle menu item lines
        Set<MenuItemLine> lines = new HashSet<>();
        for (MenuItemLineDTO lineDTO : dto.getMenuItemLines()) {
            MenuItemLine line = createMenuItemLine(menuItem, lineDTO);
            lines.add(line);
        }
        menuItem.setMenuItemLines(lines);

        MenuItem saved = menuItemRepository.save(menuItem);
        return recalcMenuItemCost(saved);
    }

    private MenuItemLine createMenuItemLine(MenuItem menuItem, MenuItemLineDTO lineDTO) {
        MenuItemLine line = new MenuItemLine();
        line.setParentMenuItem(menuItem);

        // Handle reference type
        if (lineDTO.getInventoryItemId() != null) {
            InventoryItem item = inventoryItemRepository.findById(lineDTO.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("Inventory item not found: " + lineDTO.getInventoryItemId()));
            line.setInventoryItem(item);
        }
        else if (lineDTO.getSubRecipeId() != null) {
            SubRecipe sub = subRecipeRepository.findById(lineDTO.getSubRecipeId())
                    .orElseThrow(() -> new RuntimeException("Subrecipe not found: " + lineDTO.getSubRecipeId()));
            line.setSubRecipe(sub);
        }
        else if (lineDTO.getChildMenuItemId() != null) {
            MenuItem child = menuItemRepository.findById(lineDTO.getChildMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + lineDTO.getChildMenuItemId()));
            line.setChildMenuItem(child);
        } else {
            throw new RuntimeException("Line must reference an inventory item, subrecipe, or child menu item");
        }

        // Handle UOM
        UnitOfMeasure uom = unitOfMeasureRepository.findById(lineDTO.getUnitOfMeasureId())
                .orElseThrow(() -> new RuntimeException("UOM not found: " + lineDTO.getUnitOfMeasureId()));
        line.setUnitOfMeasure(uom);

        line.setQuantity(lineDTO.getQuantity());
        line.setWastagePercent(lineDTO.getWastagePercent());
        return line;
    }


    @Override
    @Transactional
    public MenuItem updateMenuItem(Long companyId, Long menuItemId, MenuItemCreateDTO dto) {
        MenuItem existing = menuItemRepository.findByIdAndCompanyId(menuItemId, companyId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));

        // Update main fields
        existing.setName(dto.getName());
        existing.setPosCode(dto.getPosCode());
        existing.setRetailPriceExclTax(dto.getRetailPriceExclTax());
        existing.setMaxAllowedFoodCostPct(dto.getMaxAllowedFoodCostPct());
        existing.setModifierGroups(dto.getModifierGroups());

        // Update category
        Category category = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
        existing.setCategory(category);

        // Handle lines - full replace
        existing.getMenuItemLines().clear();
        for (MenuItemLineDTO lineDTO : dto.getMenuItemLines()) {
            MenuItemLine line = createMenuItemLine(existing, lineDTO);
            existing.getMenuItemLines().add(line);
        }

        return recalcMenuItemCost(menuItemRepository.save(existing));
    }


    @Override
    @Transactional
    public MenuItem partialUpdateMenuItem(Long companyId, Long menuItemId, MenuItemCreateDTO dto) {
        MenuItem existing = menuItemRepository.findByIdAndCompanyId(menuItemId, companyId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));

        // Partial field updates
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getPosCode() != null) existing.setPosCode(dto.getPosCode());
        if (dto.getRetailPriceExclTax() != null) existing.setRetailPriceExclTax(dto.getRetailPriceExclTax());
        if (dto.getMaxAllowedFoodCostPct() != null) existing.setMaxAllowedFoodCostPct(dto.getMaxAllowedFoodCostPct());
        if (dto.getModifierGroups() != null) existing.setModifierGroups(dto.getModifierGroups());

        // Partial category update
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findByCompanyIdAndId(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));
            existing.setCategory(category);
        }

        // Handle partial lines update
        if (dto.getMenuItemLines() != null) {
            existing.getMenuItemLines().clear();
            for (MenuItemLineDTO lineDTO : dto.getMenuItemLines()) {
                MenuItemLine line = createMenuItemLine(existing, lineDTO);
                existing.getMenuItemLines().add(line);
            }
        }

        return recalcMenuItemCost(menuItemRepository.save(existing));
    }


    @Override
    @Transactional
    public void deleteMenuItemById(Long companyId, Long menuItemId) {
        MenuItem existing = menuItemRepository.findByIdAndCompanyId(menuItemId, companyId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));
        menuItemRepository.delete(existing);
    }

    @Override
    @Transactional
    public MenuItem addLineToMenuItem(Long companyId, Long menuItemId, MenuItemLineDTO lineDTO) {
        MenuItem menuItem = menuItemRepository.findByIdAndCompanyId(menuItemId, companyId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));

        MenuItemLine line = createMenuItemLine(menuItem, lineDTO);
        menuItem.getMenuItemLines().add(line);
        return recalcMenuItemCost(menuItemRepository.save(menuItem));
    }

    @Override
    @Transactional
    public MenuItem removeLineFromMenuItem(Long companyId, Long menuItemId, Long lineId) {
        MenuItem menuItem = menuItemRepository.findByIdAndCompanyId(menuItemId, companyId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));

        menuItem.getMenuItemLines().removeIf(line -> line.getId().equals(lineId));
        return recalcMenuItemCost(menuItemRepository.save(menuItem));
    }



    private double calculateInventoryItemCost(MenuItemLine line) {
        InventoryItem item = line.getInventoryItem();
        UnitOfMeasure lineUom = line.getUnitOfMeasure();
        UnitOfMeasure itemUom = item.getInventoryUom();

        validateUOMs(lineUom, itemUom);

        double grossQty = line.getQuantity() * (1.0 + Optional.ofNullable(line.getWastagePercent()).orElse(0.0));
        double convertedQty = grossQty * (lineUom.getConversionFactor() / itemUom.getConversionFactor());

        return convertedQty * item.getCurrentPrice();
    }

    private double calculateSubRecipeCost(MenuItemLine line) {
        SubRecipe subRecipe = line.getSubRecipe();
        UnitOfMeasure lineUom = line.getUnitOfMeasure();
        UnitOfMeasure recipeUom = subRecipe.getUom();

        validateUOMs(lineUom, recipeUom);

        double grossQty = line.getQuantity() * (1.0 + Optional.ofNullable(line.getWastagePercent()).orElse(0.0));
        double convertedQty = grossQty * (lineUom.getConversionFactor() / recipeUom.getConversionFactor());

        // Consider sub-recipe yield
        double yield = subRecipe.getYieldQty() != null ? subRecipe.getYieldQty() : 1.0;
        return (convertedQty / yield) * subRecipe.getCost();
    }


    private void validateUOMs(UnitOfMeasure uom1, UnitOfMeasure uom2) {
        if (uom1 == null || uom2 == null) {
            throw new IllegalStateException("Both UOMs must be specified");
        }
        if (uom1.getConversionFactor() <= 0 || uom2.getConversionFactor() <= 0) {
            throw new IllegalStateException("UOM conversion factors must be positive");
        }
        if (!uom1.getCategory().getId().equals(uom2.getCategory().getId())) {
            throw new IllegalStateException("UOM categories must match for conversion");
        }
    }

    private double calculateChildMenuItemCost(MenuItemLine line, Long companyId) {
        MenuItem child = line.getChildMenuItem();
        UnitOfMeasure lineUom = line.getUnitOfMeasure();

        // Try to find EA UOM, or create a temporary one with conversion factor 1
        UnitOfMeasure childUom = unitOfMeasureRepository.findByCompanyIdAndAbbreviation(companyId, "EA")
                .orElseGet(() -> {
                    UnitOfMeasure tempUom = new UnitOfMeasure();
                    tempUom.setAbbreviation("EA");
                    tempUom.setName("Each");
                    tempUom.setConversionFactor(1.0);
                    tempUom.setCategory(lineUom.getCategory()); // Use line UOM's category
                    return tempUom;
                });

        validateUOMs(lineUom, childUom);

        double convertedQty = line.getQuantity() *
                (lineUom.getConversionFactor() / childUom.getConversionFactor());
        return convertedQty * child.getCost();
    }

    private double calculateLineCost(MenuItemLine line) {
        // Get company ID from the parent menu item
        Long companyId = line.getParentMenuItem().getCompany().getId();

        if (line.getInventoryItem() != null) {
            return calculateInventoryItemCost(line);
        } else if (line.getSubRecipe() != null) {
            return calculateSubRecipeCost(line);
        } else if (line.getChildMenuItem() != null) {
            return calculateChildMenuItemCost(line, companyId);
        }
        throw new IllegalStateException("Invalid line reference type");
    }

    // Modified recalc method to save line costs
    @Override
    @Transactional
    public MenuItem recalcMenuItemCost(MenuItem menuItem) {
        double totalCost = 0.0;

        for (MenuItemLine line : menuItem.getMenuItemLines()) {
            double lineCost = calculateLineCost(line);
            line.setLineCost(lineCost);
            totalCost += lineCost;
        }

        menuItem.setCost(totalCost);

        if (menuItem.getRetailPriceExclTax() != null && menuItem.getRetailPriceExclTax() > 0) {
            double foodCostPct = (totalCost / menuItem.getRetailPriceExclTax()) * 100;
            menuItem.setFoodCostPercentage(foodCostPct);
        } else {
            menuItem.setFoodCostPercentage(null);
        }

        return menuItemRepository.save(menuItem);
    }
    @Override
    @Transactional
    public MenuItem updateLine(Long companyId, Long menuItemId, MenuItemLineDTO dto) {
        MenuItem menuItem = menuItemRepository.findByIdAndCompanyId(menuItemId, companyId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        MenuItemLine line = menuItem.getMenuItemLines().stream()
                .filter(l -> l.getId().equals(dto.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Line not found"));

        // Update line fields
        line.setQuantity(dto.getQuantity());
        line.setWastagePercent(dto.getWastagePercent());

        // Handle UOM update
        UnitOfMeasure uom = unitOfMeasureRepository.findById(dto.getUnitOfMeasureId())
                .orElseThrow(() -> new RuntimeException("UOM not found"));
        line.setUnitOfMeasure(uom);

        // Handle reference updates if needed
        // (similar logic to createMenuItemLine)

        return recalcMenuItemCost(menuItemRepository.save(menuItem));
    }
    @Override
    public List<MenuItem> searchMenuItems(Long companyId, String searchTerm) {
        return menuItemRepository.searchMenuItems(companyId, searchTerm);
    }

}
