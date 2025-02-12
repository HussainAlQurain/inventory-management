package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.service.CategoryService;
import com.rayvision.inventory_management.service.InventoryItemService;
import com.rayvision.inventory_management.service.SupplierService;
import com.rayvision.inventory_management.service.UnitOfMeasureService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class InventoryItemFacade {
    private final InventoryItemService inventoryItemService;
    private final CategoryService categoryService;
    private final UnitOfMeasureService unitOfMeasureService;
    private final SupplierService supplierService;
    private final ModelMapper modelMapper;

    public InventoryItemFacade(InventoryItemService inventoryItemService, CategoryService categoryService, UnitOfMeasureService unitOfMeasureService, SupplierService supplierService, ModelMapper modelMapper) {
        this.inventoryItemService = inventoryItemService;
        this.categoryService = categoryService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.supplierService = supplierService;
        this.modelMapper = modelMapper;
    }

    /**
     * Creates a new InventoryItem from the provided DTO.
     * The method will:
     *  - Either select an existing Category (if categoryId is provided)
     *    or create a new one if the category details are provided.
     *  - Either select an existing UnitOfMeasure (if inventoryUomId is provided)
     *    or create a new one if the uom details are provided.
     *  - Process any nested PurchaseOptionDTOs.
     *
     * @param companyId the ID of the company (tenant)
     * @param dto       the DTO for creating the inventory item
     * @return the persisted InventoryItem
     */
    @Transactional
    public InventoryItem createInventoryItem(Long companyId, InventoryItemCreateDTO dto) {

        // 1. Map the basic fields from DTO to InventoryItem.
        InventoryItem item = modelMapper.map(dto, InventoryItem.class);

        // 2. Handle Category:
        if (dto.getCategoryId() != null) {
            // Fetch existing category.
            Category category = categoryService.getCategoryById(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found for id: " + dto.getCategoryId()));
            item.setCategory(category);
        } else if (dto.getCategory() != null) {
            // Create new category from DTO details.
            Category newCategory = modelMapper.map(dto.getCategory(), Category.class);
            newCategory = categoryService.save(companyId, newCategory);
            item.setCategory(newCategory);
        }

        // 3. Handle Unit of Measure (UOM):
        if (dto.getInventoryUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureService.getById(companyId, dto.getInventoryUomId())
                    .orElseThrow(() -> new RuntimeException("UnitOfMeasure not found for id: " + dto.getInventoryUomId()));
            item.setInventoryUom(uom);
        } else if (dto.getInventoryUom() != null) {
            UnitOfMeasure newUom = modelMapper.map(dto.getInventoryUom(), UnitOfMeasure.class);
            newUom = unitOfMeasureService.save(companyId, newUom);
            item.setInventoryUom(newUom);
        }

        // 4. Process Purchase Options:
        if (dto.getPurchaseOptions() != null && !dto.getPurchaseOptions().isEmpty()) {
            List<PurchaseOption> purchaseOptionList = new ArrayList<>();
            for (PurchaseOptionDTO poDto : dto.getPurchaseOptions()) {
                // Map basic fields from PurchaseOptionDTO to PurchaseOption.
                PurchaseOption po = modelMapper.map(poDto, PurchaseOption.class);

                // Handle the nested Supplier for this purchase option:
                if (poDto.getSupplierId() != null) {
                    Supplier supplier = supplierService.getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() -> new RuntimeException("Supplier not found for id: " + poDto.getSupplierId()));
                    po.setSupplier(supplier);
                } else if (poDto.getSupplier() != null) {
                    Supplier newSupplier = modelMapper.map(poDto.getSupplier(), Supplier.class);
                    newSupplier = supplierService.save(companyId, newSupplier);
                    po.setSupplier(newSupplier);
                }
                // Set the purchase option's parent InventoryItem later
                purchaseOptionList.add(po);
            }
            // Set the collection and ensure bidirectional mapping.
            item.setPurchaseOptions(new HashSet<>(purchaseOptionList));
            for (PurchaseOption po : item.getPurchaseOptions()) {
                po.setInventoryItem(item);
            }
        }

        // 5. Save the complete InventoryItem (cascading purchase options, etc.)
        return inventoryItemService.save(companyId, item);
    }

}
