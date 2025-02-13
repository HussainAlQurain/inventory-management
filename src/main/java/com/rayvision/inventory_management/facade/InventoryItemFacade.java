package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.mappers.impl.InventoryItemCreateMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.service.CategoryService;
import com.rayvision.inventory_management.service.InventoryItemService;
import com.rayvision.inventory_management.service.SupplierService;
import com.rayvision.inventory_management.service.UnitOfMeasureService;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryItemFacade {

    private final InventoryItemService inventoryItemService;
    private final CategoryService categoryService;
    private final UnitOfMeasureService unitOfMeasureService;
    private final SupplierService supplierService;
    private final InventoryItemCreateMapper itemCreateMapper;

    public InventoryItemFacade(InventoryItemService inventoryItemService,
                               CategoryService categoryService,
                               UnitOfMeasureService unitOfMeasureService,
                               SupplierService supplierService,
                               InventoryItemCreateMapper itemCreateMapper) {
        this.inventoryItemService = inventoryItemService;
        this.categoryService = categoryService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.supplierService = supplierService;
        this.itemCreateMapper = itemCreateMapper;
    }

    /**
     * Creates a new InventoryItem from the provided DTO.
     * This method will:
     *  - Use the dedicated mapper (which skips purchaseOptions) for the basic fields.
     *  - Either select an existing Category (if categoryId is provided) or create a new one.
     *  - Either select an existing UnitOfMeasure (if inventoryUomId is provided) or create a new one.
     *  - Manually process any nested PurchaseOptionDTOs, including handling the nested Supplier.
     *
     * @param companyId the ID of the company (tenant)
     * @param dto       the DTO for creating the inventory item
     * @return the persisted InventoryItem
     */
    @Transactional
    public InventoryItem createInventoryItem(Long companyId, InventoryItemCreateDTO dto) {
        // 1. Use the dedicated mapper to map basic fields (purchaseOptions are skipped).
        InventoryItem item = itemCreateMapper.mapFrom(dto);

        // 2. Handle Category:
        if (dto.getCategoryId() != null) {
            Category existingCategory = categoryService.getCategoryById(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found for id: " + dto.getCategoryId()));
            item.setCategory(existingCategory);
        } else if (dto.getCategory() != null) {
            Category newCategory = itemCreateMapper.getModelMapper().map(dto.getCategory(), Category.class);
            newCategory = categoryService.save(companyId, newCategory);
            item.setCategory(newCategory);
        }

        // 3. Handle Unit of Measure (UOM):
        if (dto.getInventoryUomId() != null) {
            UnitOfMeasure existingUom = unitOfMeasureService.getById(companyId, dto.getInventoryUomId())
                    .orElseThrow(() -> new RuntimeException("UnitOfMeasure not found for id: " + dto.getInventoryUomId()));
            item.setInventoryUom(existingUom);
        } else if (dto.getInventoryUom() != null) {
            UnitOfMeasure newUom = itemCreateMapper.getModelMapper().map(dto.getInventoryUom(), UnitOfMeasure.class);
            newUom = unitOfMeasureService.save(companyId, newUom);
            item.setInventoryUom(newUom);
        }

        // 4. Manually process PurchaseOptions:
        if (dto.getPurchaseOptions() != null && !dto.getPurchaseOptions().isEmpty()) {
            Set<PurchaseOption> purchaseOptions = new HashSet<>();
            for (PurchaseOptionDTO poDto : dto.getPurchaseOptions()) {
                PurchaseOption po = new PurchaseOption();
                // Map simple fields manually:
                po.setPrice(poDto.getPrice());
                po.setTaxRate(poDto.getTaxRate());
                po.setInnerPackQuantity(poDto.getInnerPackQuantity());
                po.setPacksPerCase(poDto.getPacksPerCase());
                po.setMinOrderQuantity(poDto.getMinOrderQuantity());
                po.setMainPurchaseOption(poDto.isMainPurchaseOption());
                po.setOrderingEnabled(poDto.isOrderingEnabled());
                po.setSupplierProductCode(poDto.getSupplierProductCode());
                po.setNickname(poDto.getNickname());
                po.setScanBarcode(poDto.getScanBarcode());

                // Handle orderingUom if provided:
                if (poDto.getOrderingUomId() != null) {
                    UnitOfMeasure orderingUom = unitOfMeasureService.getById(companyId, poDto.getOrderingUomId())
                            .orElseThrow(() -> new RuntimeException("Ordering UOM not found for id: " + poDto.getOrderingUomId()));
                    po.setOrderingUom(orderingUom);
                }
                // Handle nested Supplier:
                if (poDto.getSupplierId() != null) {
                    Supplier existingSupplier = supplierService.getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() -> new RuntimeException("Supplier not found for id: " + poDto.getSupplierId()));
                    po.setSupplier(existingSupplier);
                } else if (poDto.getSupplier() != null) {
                    Supplier newSupplier = itemCreateMapper.getModelMapper().map(poDto.getSupplier(), Supplier.class);
                    newSupplier = supplierService.save(companyId, newSupplier);
                    po.setSupplier(newSupplier);
                }

                // Link the purchase option to the inventory item
                po.setInventoryItem(item);
                purchaseOptions.add(po);
            }
            item.setPurchaseOptions(purchaseOptions);
        }

        // 5. Save the complete InventoryItem (with cascading on purchaseOptions)
        return inventoryItemService.save(companyId, item);
    }

}
