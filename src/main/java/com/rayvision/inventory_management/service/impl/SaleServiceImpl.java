package com.rayvision.inventory_management.service.impl;


import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.SaleCreateDTO;
import com.rayvision.inventory_management.model.dto.SaleLineDTO;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.MenuItemRepository;
import com.rayvision.inventory_management.repository.SaleRepository;
import com.rayvision.inventory_management.service.SaleService;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final LocationRepository locationRepository;
    private final MenuItemRepository menuItemRepository;
    private final StockTransactionService stockTransactionService;

    public SaleServiceImpl(SaleRepository saleRepository,
                           LocationRepository locationRepository,
                           MenuItemRepository menuItemRepository,
                           StockTransactionService stockTransactionService) {
        this.saleRepository = saleRepository;
        this.locationRepository = locationRepository;
        this.menuItemRepository = menuItemRepository;
        this.stockTransactionService = stockTransactionService;
    }

    @Override
    public Sale createSale(SaleCreateDTO dto) {
        // 1) Validate location
        Location location = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found: " + dto.getLocationId()));

        // 2) Build the Sale
        Sale sale = new Sale();
        sale.setLocation(location);
        sale.setSaleDateTime(dto.getSaleDateTime() != null ? dto.getSaleDateTime() : LocalDateTime.now());
        sale.setPosReference(dto.getPosReference());

        double totalRevenue = 0.0;
        double totalCost    = 0.0;

        // We'll add lines below
        List<SaleLine> saleLines = new ArrayList<>();

        for (SaleLineDTO lineDto : dto.getLines()) {
            // a) find or create the MenuItem
            MenuItem menuItem = findOrCreateMenuItemByPosCode(lineDto.getPosCode(), lineDto.getMenuItemName());

            // b) compute cost for (menuItem, quantity)
            double costForThisLine = computeMenuItemCostAtSaleTime(menuItem, lineDto.getQuantity());
            double extendedPrice   = lineDto.getUnitPrice() * lineDto.getQuantity();

            // c) create the usage stock transactions for underlying ingredients
            expandMenuItemUsage(menuItem, lineDto.getQuantity(), location, sale.getId());

            // d) build the line
            SaleLine line = new SaleLine();
            line.setSale(sale);
            line.setMenuItem(menuItem);
            line.setPosCode(lineDto.getPosCode());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPriceAtSale(lineDto.getUnitPrice());
            line.setExtendedPrice(extendedPrice);
            line.setCostAtSaleTime(costForThisLine);
            line.setProfitAtSaleTime(extendedPrice - costForThisLine);

            saleLines.add(line);

            totalRevenue += extendedPrice;
            totalCost    += costForThisLine;
        }

        sale.setLines(saleLines);
        sale.setTotalRevenue(totalRevenue);
        sale.setTotalCost(totalCost);
        sale.setTotalProfit(totalRevenue - totalCost);

        return saleRepository.save(sale);
    }

    /**
     * Example function that expands a MenuItem's lines to do usage transactions.
     * If the MenuItem has children lines referencing subRecipes or child menuItems,
     * we recursively expand them.
     */
    private void expandMenuItemUsage(MenuItem menuItem, double saleQty, Location location, Long saleId) {
        // for each line in menuItem.getMenuItemLines()
        // - compute the "gross usage" based on quantity + wastage
        // - if inventoryItem => stockTransactionService.recordUsage(...)
        // - if subRecipe => expand usage for that subRecipe's lines OR recordUsage if the sub is tracked
        // - if childMenuItem => recursively expand usage
        for (MenuItemLine line : menuItem.getMenuItemLines()) {
            double wastageFraction = Optional.ofNullable(line.getWastagePercent()).orElse(0.0) / 100.0;
            // The line quantity is "per 1 menu item" => total usage = lineQty * saleQty
            double grossQty = line.getQuantity() * saleQty * (1 + wastageFraction);

            if (line.getInventoryItem() != null) {
                // outflow of this item
                stockTransactionService.recordUsage(
                        location,
                        line.getInventoryItem(),
                        grossQty,
                        line.getUnitOfMeasure(),
                        saleId,   // sourceReference
                        LocalDate.now()
                );
            }
            else if (line.getSubRecipe() != null) {
                // you might do recordUsage for subRecipe
                stockTransactionService.recordUsage(
                        location,
                        line.getSubRecipe(),
                        grossQty,
                        line.getUnitOfMeasure(),
                        saleId,
                        LocalDate.now()
                );
                // or if you want to expand subRecipe's lines, you'd do something similar to recursion
            }
            else if (line.getChildMenuItem() != null) {
                // recursion
                expandMenuItemUsage(line.getChildMenuItem(), grossQty, location, saleId);
            }
            else {
                throw new RuntimeException("Invalid line reference for MenuItemLine " + line.getId());
            }
        }
    }

    /**
     * Example function to compute cost for a menu item * quantity sold * at that moment*.
     * We do basically the same logic as recalcMenuItemCost(...) but for saleQty.
     */
    private double computeMenuItemCostAtSaleTime(MenuItem menuItem, double saleQty) {
        // We'll sum up cost from each line
        double total = 0.0;
        for (MenuItemLine line : menuItem.getMenuItemLines()) {
            // each line has .getQuantity() per 1.0 item
            // total usage = lineQty * saleQty
            double wastageFraction = Optional.ofNullable(line.getWastagePercent()).orElse(0.0) / 100.0;
            double grossQty = line.getQuantity() * saleQty * (1 + wastageFraction);

            if (line.getInventoryItem() != null) {
                // cost = grossQty in line's UOM => convert to item base => multiply by item price
                double cost = getInventoryItemCost(line.getInventoryItem(), grossQty, line.getUnitOfMeasure());
                total += cost;
            }
            else if (line.getSubRecipe() != null) {
                double cost = getSubRecipeCost(line.getSubRecipe(), grossQty, line.getUnitOfMeasure());
                total += cost;
            }
            else if (line.getChildMenuItem() != null) {
                // recursion or just childMenuItem.getCost() * ...
                double childCostPer1 = line.getChildMenuItem().getCost() != null ? line.getChildMenuItem().getCost() : 0.0;
                total += (childCostPer1 * grossQty);
            }
        }
        return total;
    }

    private double getInventoryItemCost(InventoryItem item, double grossQty, UnitOfMeasure lineUom) {
        // same as your itemCostCalculator
        UnitOfMeasure itemUom = item.getInventoryUom();
        double ratio = lineUom.getConversionFactor() / itemUom.getConversionFactor();
        double baseQty = grossQty * ratio;
        double pricePerBase = Optional.ofNullable(item.getCurrentPrice()).orElse(0.0);
        return baseQty * pricePerBase;
    }

    private double getSubRecipeCost(SubRecipe sub, double grossQty, UnitOfMeasure lineUom) {
        // same as your subRecipeCost logic
        UnitOfMeasure subBaseUom = sub.getUom();
        double ratio = lineUom.getConversionFactor() / subBaseUom.getConversionFactor();
        double subBaseQty = grossQty * ratio;
        double yield = (sub.getYieldQty() != null) ? sub.getYieldQty() : 1.0;
        double costPerBase = (sub.getCost() != null) ? sub.getCost() : 0.0;
        return (subBaseQty / yield) * costPerBase;
    }

    /**
     * If the system does not find a matching MenuItem by posCode,
     * it can create a “placeholder” item or do partial sync.
     */
    private MenuItem findOrCreateMenuItemByPosCode(String posCode, String fallbackName) {
        MenuItem item = menuItemRepository.findByPosCode(posCode).orElse(null);
        if (item == null) {
            // create a placeholder
            item = new MenuItem();
            item.setName(fallbackName != null ? fallbackName : posCode);
            item.setPosCode(posCode);
            item.setRetailPriceExclTax(0.0); // user can fix later
            item.setCompany(null); // or find a default company if you want
            item = menuItemRepository.save(item);
        }
        return item;
    }

    @Override
    public List<Sale> findSalesByLocationAndDateRange(Long locationId, LocalDateTime start, LocalDateTime end) {
        return saleRepository.findAllByLocationIdAndSaleDateTimeBetween(locationId, start, end);
    }

    @Override
    public Optional<Sale> getSaleById(Long saleId) {
        return saleRepository.findById(saleId);
    }


}
