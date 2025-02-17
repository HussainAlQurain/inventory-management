package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.mappers.impl.*;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCreateDTO;
import com.rayvision.inventory_management.service.CategoryService;
import com.rayvision.inventory_management.service.InventoryItemService;
import com.rayvision.inventory_management.service.SupplierService;
import com.rayvision.inventory_management.service.UnitOfMeasureCategoryService;
import com.rayvision.inventory_management.service.UnitOfMeasureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class InventoryItemFacade {

    private final InventoryItemService inventoryItemService;
    private final CategoryService categoryService;
    private final UnitOfMeasureService unitOfMeasureService;
    private final UnitOfMeasureCategoryService unitOfMeasureCategoryService;
    private final SupplierService supplierService;

    private final CategoryMapper categoryMapper;
    private final UnitOfMeasureMapper unitOfMeasureMapper;
    private final UnitOfMeasureCategoryMapper unitOfMeasureCategoryMapper;
    private final PurchaseOptionMapper purchaseOptionMapper;
    private final SupplierMapper supplierMapper;

    public InventoryItemFacade(InventoryItemService inventoryItemService,
                               CategoryService categoryService,
                               UnitOfMeasureService unitOfMeasureService,
                               UnitOfMeasureCategoryService unitOfMeasureCategoryService,
                               SupplierService supplierService,
                               CategoryMapper categoryMapper,
                               UnitOfMeasureMapper unitOfMeasureMapper,
                               UnitOfMeasureCategoryMapper unitOfMeasureCategoryMapper,
                               PurchaseOptionMapper purchaseOptionMapper,
                               SupplierMapper supplierMapper) {
        this.inventoryItemService = inventoryItemService;
        this.categoryService = categoryService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.unitOfMeasureCategoryService = unitOfMeasureCategoryService;
        this.supplierService = supplierService;
        this.categoryMapper = categoryMapper;
        this.unitOfMeasureMapper = unitOfMeasureMapper;
        this.unitOfMeasureCategoryMapper = unitOfMeasureCategoryMapper;
        this.purchaseOptionMapper = purchaseOptionMapper;
        this.supplierMapper = supplierMapper;
    }

    @Transactional
    public InventoryItem createInventoryItem(Long companyId, InventoryItemCreateDTO inventoryItemCreateDTO) {
        // Create a new InventoryItem and copy basic fields (without nested objects).
        InventoryItem item = new InventoryItem();
        item.setName(inventoryItemCreateDTO.getName());
        item.setSku(inventoryItemCreateDTO.getSku());
        item.setProductCode(inventoryItemCreateDTO.getProductCode());
        item.setDescription(inventoryItemCreateDTO.getDescription());
        item.setCurrentPrice(inventoryItemCreateDTO.getCurrentPrice());
        item.setCalories(inventoryItemCreateDTO.getCalories());

        // Process Category:
        if (inventoryItemCreateDTO.getCategoryId() != null) {
            Category cat = categoryService.getCategoryById(companyId, inventoryItemCreateDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found for id: " + inventoryItemCreateDTO.getCategoryId()));
            item.setCategory(cat);
        } else if (inventoryItemCreateDTO.getCategory() != null) {
            Category cat = categoryMapper.mapFrom(inventoryItemCreateDTO.getCategory());
            cat = categoryService.save(companyId, cat);
            item.setCategory(cat);
        }

        // Process Inventory Unit of Measure:
        if (inventoryItemCreateDTO.getInventoryUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureService.getById(companyId, inventoryItemCreateDTO.getInventoryUomId())
                    .orElseThrow(() -> new RuntimeException("Inventory UOM not found for id: " + inventoryItemCreateDTO.getInventoryUomId()));
            item.setInventoryUom(uom);
        } else if (inventoryItemCreateDTO.getInventoryUom() != null) {
            UnitOfMeasureCreateDTO uomDto = inventoryItemCreateDTO.getInventoryUom();
            UnitOfMeasure uom = unitOfMeasureMapper.mapFrom(uomDto);
            // Process nested UOM Category:
            if (uomDto.getCategoryId() != null) {
                UnitOfMeasureCategory uomCat = unitOfMeasureCategoryService.findById(uomDto.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("UOM Category not found for id: " + uomDto.getCategoryId()));
                uom.setCategory(uomCat);
            } else if (uomDto.getCategory() != null) {
                UnitOfMeasureCategory uomCat = unitOfMeasureCategoryMapper.mapFrom(uomDto.getCategory());
                uomCat = unitOfMeasureCategoryService.save(companyId, uomCat);
                uom.setCategory(uomCat);
            }
            uom = unitOfMeasureService.save(companyId, uom);
            item.setInventoryUom(uom);
        }

        // Process Purchase Options:
        if (inventoryItemCreateDTO.getPurchaseOptions() != null && !inventoryItemCreateDTO.getPurchaseOptions().isEmpty()) {
            Set<PurchaseOption> options = new HashSet<>();
            for (PurchaseOptionDTO poDto : inventoryItemCreateDTO.getPurchaseOptions()) {
                PurchaseOption po = purchaseOptionMapper.mapFrom(poDto); // maps simple fields
                // Process ordering UOM:
                if (poDto.getOrderingUomId() != null) {
                    UnitOfMeasure orderingUom = unitOfMeasureService.getById(companyId, poDto.getOrderingUomId())
                            .orElseThrow(() -> new RuntimeException("Ordering UOM not found for id: " + poDto.getOrderingUomId()));
                    po.setOrderingUom(orderingUom);
                } else if (poDto.getOrderingUom() != null) {
                    UnitOfMeasureCreateDTO orderingUomDto = poDto.getOrderingUom();
                    UnitOfMeasure orderingUom = unitOfMeasureMapper.mapFrom(orderingUomDto);
                    if (orderingUomDto.getCategoryId() != null) {
                        UnitOfMeasureCategory ordCat = unitOfMeasureCategoryService.findById(orderingUomDto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Ordering UOM Category not found for id: " + orderingUomDto.getCategoryId()));
                        orderingUom.setCategory(ordCat);
                    } else if (orderingUomDto.getCategory() != null) {
                        UnitOfMeasureCategory ordCat = unitOfMeasureCategoryMapper.mapFrom(orderingUomDto.getCategory());
                        ordCat = unitOfMeasureCategoryService.save(companyId, ordCat);
                        orderingUom.setCategory(ordCat);
                    }
                    orderingUom = unitOfMeasureService.save(companyId, orderingUom);
                    po.setOrderingUom(orderingUom);
                } else {
                    // Default ordering UOM is the InventoryItem's UOM.
                    po.setOrderingUom(item.getInventoryUom());
                }
                // Process Supplier:
                if (poDto.getSupplierId() != null) {
                    Supplier supp = supplierService.getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() -> new RuntimeException("Supplier not found for id: " + poDto.getSupplierId()));
                    po.setSupplier(supp);
                } else if (poDto.getSupplier() != null) {
                    Supplier supp = supplierMapper.mapFrom(poDto.getSupplier());
                    // (If needed, you can also have dedicated mappers for nested emails/phones.)
                    supp = supplierService.save(companyId, supp);
                    po.setSupplier(supp);
                }
                // Link back to the InventoryItem.
                po.setInventoryItem(item);
                options.add(po);
            }
            item.setPurchaseOptions(options);
        }

        // Finally, save the InventoryItem (with cascade for purchase options if configured).
        return inventoryItemService.save(companyId, item);
    }
}
