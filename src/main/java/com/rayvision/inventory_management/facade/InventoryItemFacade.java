package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.mappers.impl.InventoryItemCreateMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryCreateDTO;
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
    private final InventoryItemCreateMapper itemCreateMapper;

    public InventoryItemFacade(InventoryItemService inventoryItemService,
                               CategoryService categoryService,
                               UnitOfMeasureService unitOfMeasureService,
                               UnitOfMeasureCategoryService unitOfMeasureCategoryService,
                               SupplierService supplierService,
                               InventoryItemCreateMapper itemCreateMapper) {
        this.inventoryItemService = inventoryItemService;
        this.categoryService = categoryService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.unitOfMeasureCategoryService = unitOfMeasureCategoryService;
        this.supplierService = supplierService;
        this.itemCreateMapper = itemCreateMapper;
    }

    /**
     * Creates a new InventoryItem from the provided DTO.
     *
     * <ul>
     *   <li>Uses the dedicated mapper (which skips category, inventoryUom, and purchaseOptions) to map basic fields.</li>
     *   <li>Handles the InventoryItem’s category:
     *       <ul>
     *         <li>If a categoryId is provided, fetch that category.</li>
     *         <li>Otherwise, if category details are provided, map and save a new Category.</li>
     *       </ul>
     *   </li>
     *   <li>Handles the InventoryItem’s UOM:
     *       <ul>
     *         <li>If an inventoryUomId is provided, fetch that UOM.</li>
     *         <li>Otherwise, maps the provided UOM DTO.
     *             <ul>
     *                <li>If the nested UOM category is provided and its id is null, either use its categoryId (if given) or map and save a new UnitOfMeasureCategory.</li>
     *             </ul>
     *         </li>
     *       </ul>
     *   </li>
     *   <li>Processes each PurchaseOption similarly:
     *       <ul>
     *         <li>Maps simple fields.</li>
     *         <li>Handles ordering UOM:
     *             <ul>
     *               <li>If an orderingUomId is provided, fetch that UOM.</li>
     *               <li>Else if orderingUom details are provided, map and save a new UOM (handling its nested category as above).</li>
     *               <li>If neither is provided, defaults to the InventoryItem’s UOM.</li>
     *             </ul>
     *         </li>
     *         <li>Handles the nested Supplier:
     *             <ul>
     *               <li>If a supplierId is provided, fetch that supplier.</li>
     *               <li>Otherwise, map and save a new Supplier (making sure to set the supplier reference on its nested emails and phones).</li>
     *             </ul>
     *         </li>
     *         <li>Links the purchase option back to the InventoryItem.</li>
     *       </ul>
     *   </li>
     *   <li>Saves the entire InventoryItem (with cascading on purchase options, etc.).</li>
     * </ul>
     *
     * @param companyId the ID of the company (tenant)
     * @param dto       the DTO for creating the inventory item
     * @return the persisted InventoryItem
     */
    @Transactional
    public InventoryItem createInventoryItem(Long companyId, InventoryItemCreateDTO dto) {
        // 1. Map basic fields using the dedicated mapper.
        // (Ensure that the mapper is configured to skip mapping of category, inventoryUom, and purchaseOptions.)
        InventoryItem item = itemCreateMapper.mapFrom(dto);

        // 2. Handle InventoryItem's Category.
        if (dto.getCategoryId() != null) {
            Category existingCategory = categoryService.getCategoryById(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem category not found for id: " + dto.getCategoryId()));
            item.setCategory(existingCategory);
        } else if (dto.getCategory() != null) {
            // Map and save a new Category.
            Category newCategory = itemCreateMapper.getModelMapper().map(dto.getCategory(), Category.class);
            newCategory = categoryService.save(companyId, newCategory);
            item.setCategory(newCategory);
        }

        // 3. Handle InventoryItem's UOM.
        if (dto.getInventoryUomId() != null) {
            UnitOfMeasure existingUom = unitOfMeasureService.getById(companyId, dto.getInventoryUomId())
                    .orElseThrow(() -> new RuntimeException(
                            "Inventory UOM not found for id: " + dto.getInventoryUomId()));
            item.setInventoryUom(existingUom);
        } else if (dto.getInventoryUom() != null) {
            UnitOfMeasureCreateDTO uomDto = dto.getInventoryUom();
            UnitOfMeasure newUom = itemCreateMapper.getModelMapper().map(uomDto, UnitOfMeasure.class);

            // Manually handle the nested UOM category.
            if (uomDto.getCategory() != null) {
                if (uomDto.getCategoryId() != null) {
                    UnitOfMeasureCategory existingUomCat = unitOfMeasureCategoryService
                            .findById(uomDto.getCategoryId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM Category not found for id: " + uomDto.getCategoryId()));
                    newUom.setCategory(existingUomCat);
                } else {
                    UnitOfMeasureCategoryCreateDTO catDto = uomDto.getCategory();
                    UnitOfMeasureCategory uomCategory = UnitOfMeasureCategory.builder()
                            .name(catDto.getName())
                            .description(catDto.getDescription())
                            .build();
                    UnitOfMeasureCategory savedCategory = unitOfMeasureCategoryService.save(companyId, uomCategory);
                    newUom.setCategory(savedCategory);
                }
            }
            newUom = unitOfMeasureService.save(companyId, newUom);
            item.setInventoryUom(newUom);
        }

        // 4. Process PurchaseOptions.
        if (dto.getPurchaseOptions() != null && !dto.getPurchaseOptions().isEmpty()) {
            Set<PurchaseOption> purchaseOptions = new HashSet<>();
            for (PurchaseOptionDTO poDto : dto.getPurchaseOptions()) {
                PurchaseOption po = new PurchaseOption();
                // Map simple fields.
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

                // Handle ordering UOM.
                if (poDto.getOrderingUomId() != null) {
                    UnitOfMeasure orderingUom = unitOfMeasureService.getById(companyId, poDto.getOrderingUomId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Ordering UOM not found for id: " + poDto.getOrderingUomId()));
                    po.setOrderingUom(orderingUom);
                } else if (poDto.getOrderingUom() != null) {
                    UnitOfMeasureCreateDTO orderingUomDto = poDto.getOrderingUom();
                    UnitOfMeasure newOrderingUom = itemCreateMapper.getModelMapper()
                            .map(orderingUomDto, UnitOfMeasure.class);

                    // Handle nested UOM category for ordering UOM.
                    if (orderingUomDto.getCategory() != null) {
                        if (orderingUomDto.getCategoryId() != null) {
                            UnitOfMeasureCategory existingOrderingCat = unitOfMeasureCategoryService
                                    .findById(orderingUomDto.getCategoryId())
                                    .orElseThrow(() -> new RuntimeException(
                                            "Ordering UOM Category not found for id: "
                                                    + orderingUomDto.getCategoryId()));
                            newOrderingUom.setCategory(existingOrderingCat);
                        } else {
                            UnitOfMeasureCategoryCreateDTO catDto = orderingUomDto.getCategory();
                            UnitOfMeasureCategory orderingCategory = UnitOfMeasureCategory.builder()
                                    .name(catDto.getName())
                                    .description(catDto.getDescription())
                                    .build();
                            UnitOfMeasureCategory savedOrderingCat =
                                    unitOfMeasureCategoryService.save(companyId, orderingCategory);
                            newOrderingUom.setCategory(savedOrderingCat);
                        }
                    }
                    newOrderingUom = unitOfMeasureService.save(companyId, newOrderingUom);
                    po.setOrderingUom(newOrderingUom);
                } else {
                    // Default to the InventoryItem's UOM.
                    po.setOrderingUom(item.getInventoryUom());
                }

                // Handle nested Supplier.
                if (poDto.getSupplierId() != null) {
                    Supplier existingSupplier = supplierService.getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Supplier not found for id: " + poDto.getSupplierId()));
                    po.setSupplier(existingSupplier);
                } else if (poDto.getSupplier() != null) {
                    // Use a final (or effectively final) variable for the mapped supplier,
                    // so we can safely use it in a lambda.
                    final Supplier mappedSupplier = itemCreateMapper.getModelMapper()
                            .map(poDto.getSupplier(), Supplier.class);

                    // Update nested emails/phones to point back to mappedSupplier
                    if (mappedSupplier.getOrderEmails() != null) {
                        mappedSupplier.getOrderEmails().forEach(e -> e.setSupplier(mappedSupplier));
                    }
                    if (mappedSupplier.getOrderPhones() != null) {
                        mappedSupplier.getOrderPhones().forEach(p -> p.setSupplier(mappedSupplier));
                    }

                    // Now persist the new supplier. This returns the managed instance.
                    Supplier savedSupplier = supplierService.save(companyId, mappedSupplier);

                    // Attach the saved (managed) instance to the PurchaseOption
                    po.setSupplier(savedSupplier);
                }

                // Link purchase option to the InventoryItem.
                po.setInventoryItem(item);
                purchaseOptions.add(po);
            }
            item.setPurchaseOptions(purchaseOptions);
        }

        // 5. Save the complete InventoryItem (cascading purchase options, etc.)
        return inventoryItemService.save(companyId, item);
    }
}
