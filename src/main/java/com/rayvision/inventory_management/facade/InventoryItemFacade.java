package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.mappers.impl.InventoryItemCreateMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryCreateDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCreateDTO;
import com.rayvision.inventory_management.service.*;
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
     * <p>The method will:</p>
     * <ul>
     *   <li>Use the dedicated mapper (which skips purchaseOptions) to map basic fields.</li>
     *   <li>Handle the InventoryItem's Category:
     *       <ul>
     *         <li>If a categoryId is provided, fetch that category.</li>
     *         <li>Otherwise, if category details are provided, map and save a new category.</li>
     *       </ul>
     *   </li>
     *   <li>Handle the InventoryItem's UOM:
     *       <ul>
     *         <li>If an inventoryUomId is provided, fetch that UOM.</li>
     *         <li>Otherwise, map and save the UOM from the provided details (including its nested category).</li>
     *       </ul>
     *   </li>
     *   <li>Process each PurchaseOption:
     *       <ul>
     *         <li>Map simple fields.</li>
     *         <li>For ordering UOM:
     *             <ul>
     *               <li>If an orderingUomId is provided, fetch that UOM.</li>
     *               <li>Else if orderingUom details are provided, map and save a new UOM (including its nested category).</li>
     *               <li>If neither is provided, default to the InventoryItem's UOM.</li>
     *             </ul>
     *         </li>
     *         <li>For the nested Supplier:
     *             <ul>
     *               <li>If a supplierId is provided, fetch that supplier.</li>
     *               <li>Otherwise, map and save a new supplier (set references on its emails/phones before saving).</li>
     *             </ul>
     *         </li>
     *         <li>Link the PurchaseOption back to the InventoryItem.</li>
     *       </ul>
     *   </li>
     *   <li>Save the entire InventoryItem (with cascading to purchase options, etc.).</li>
     * </ul>
     *
     * @param companyId the ID of the company (tenant)
     * @param dto       the DTO for creating the inventory item
     * @return the persisted InventoryItem
     */
    @Transactional
    public InventoryItem createInventoryItem(Long companyId, InventoryItemCreateDTO dto) {
        // 1) Map basic fields (purchaseOptions are skipped in the mapper).
        InventoryItem item = itemCreateMapper.mapFrom(dto);

        // 2) Handle InventoryItem's Category.
        if (dto.getCategoryId() != null) {
            // Fetch existing category
            Category existingCategory = categoryService
                    .getCategoryById(companyId, dto.getCategoryId())
                    .orElseThrow(() ->
                            new RuntimeException("InventoryItem category not found for id: " + dto.getCategoryId())
                    );
            item.setCategory(existingCategory);

        } else if (dto.getCategory() != null) {
            // Create a new category from the DTO
            Category newCategory = itemCreateMapper.getModelMapper().map(dto.getCategory(), Category.class);
            newCategory = categoryService.save(companyId, newCategory);
            item.setCategory(newCategory);
        }

        // 3) Handle InventoryItem's UOM.
        if (dto.getInventoryUomId() != null) {
            // Fetch existing UOM
            UnitOfMeasure existingUom = unitOfMeasureService
                    .getById(companyId, dto.getInventoryUomId())
                    .orElseThrow(() ->
                            new RuntimeException("Inventory UOM not found for id: " + dto.getInventoryUomId())
                    );
            item.setInventoryUom(existingUom);

        } else if (dto.getInventoryUom() != null) {
            // Map and save a new UOM from the DTO
            UnitOfMeasureCreateDTO uomDto = dto.getInventoryUom();
            UnitOfMeasure newUom = itemCreateMapper.getModelMapper().map(uomDto, UnitOfMeasure.class);

            // Check if there's a nested category
            if (uomDto.getCategory() != null) {
                if (uomDto.getCategoryId() != null) {
                    // Fetch existing UOM category by ID
                    UnitOfMeasureCategory existingCat = unitOfMeasureCategoryService
                            .findById(uomDto.getCategoryId())
                            .orElseThrow(() ->
                                    new RuntimeException("UOM Category not found for id: " + uomDto.getCategoryId())
                            );
                    newUom.setCategory(existingCat);

                } else {
                    // Create a new UOM category from the nested DTO
                    UnitOfMeasureCategoryCreateDTO catDto = uomDto.getCategory();
                    UnitOfMeasureCategory uomCategory = new UnitOfMeasureCategory();
                    uomCategory.setName(catDto.getName());
                    uomCategory.setDescription(catDto.getDescription());
                    UnitOfMeasureCategory savedCat = unitOfMeasureCategoryService.save(companyId, uomCategory);
                    newUom.setCategory(savedCat);
                }
            }

            // Save the new UOM and attach to the item
            newUom = unitOfMeasureService.save(companyId, newUom);
            item.setInventoryUom(newUom);
        }

        // 4) Process PurchaseOptions
        if (dto.getPurchaseOptions() != null && !dto.getPurchaseOptions().isEmpty()) {
            Set<PurchaseOption> purchaseOptions = new HashSet<>();

            for (PurchaseOptionDTO poDto : dto.getPurchaseOptions()) {
                PurchaseOption po = new PurchaseOption();
                // Map simple fields
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

                // Handle ordering UOM
                if (poDto.getOrderingUomId() != null) {
                    UnitOfMeasure orderingUom = unitOfMeasureService
                            .getById(companyId, poDto.getOrderingUomId())
                            .orElseThrow(() ->
                                    new RuntimeException("Ordering UOM not found for id: " + poDto.getOrderingUomId())
                            );
                    po.setOrderingUom(orderingUom);

                } else if (poDto.getOrderingUom() != null) {
                    // Create new ordering UOM
                    UnitOfMeasureCreateDTO orderingUomDto = poDto.getOrderingUom();
                    UnitOfMeasure newOrderingUom = itemCreateMapper.getModelMapper()
                            .map(orderingUomDto, UnitOfMeasure.class);

                    // If there's a nested category
                    if (orderingUomDto.getCategory() != null) {
                        if (orderingUomDto.getCategoryId() != null) {
                            UnitOfMeasureCategory existingCat = unitOfMeasureCategoryService
                                    .findById(orderingUomDto.getCategoryId())
                                    .orElseThrow(() ->
                                            new RuntimeException("Ordering UOM Category not found for id: "
                                                    + orderingUomDto.getCategoryId())
                                    );
                            newOrderingUom.setCategory(existingCat);

                        } else {
                            // Create new category for ordering UOM
                            UnitOfMeasureCategoryCreateDTO catDto = orderingUomDto.getCategory();
                            UnitOfMeasureCategory uomCategory = new UnitOfMeasureCategory();
                            uomCategory.setName(catDto.getName());
                            uomCategory.setDescription(catDto.getDescription());
                            UnitOfMeasureCategory savedCat = unitOfMeasureCategoryService
                                    .save(companyId, uomCategory);
                            newOrderingUom.setCategory(savedCat);
                        }
                    }

                    newOrderingUom = unitOfMeasureService.save(companyId, newOrderingUom);
                    po.setOrderingUom(newOrderingUom);

                } else {
                    // Default to the InventoryItem's UOM
                    po.setOrderingUom(item.getInventoryUom());
                }

                // Handle nested Supplier
                if (poDto.getSupplierId() != null) {
                    // Fetch existing supplier
                    Supplier existingSupplier = supplierService
                            .getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() ->
                                    new RuntimeException("Supplier not found for id: " + poDto.getSupplierId())
                            );
                    po.setSupplier(existingSupplier);

                } else if (poDto.getSupplier() != null) {
                    // Map the new supplier
                    Supplier mappedSupplier = itemCreateMapper.getModelMapper()
                            .map(poDto.getSupplier(), Supplier.class);

                    // Set references for each email
                    if (mappedSupplier.getOrderEmails() != null) {
                        mappedSupplier.getOrderEmails().forEach(e -> e.setSupplier(mappedSupplier));
                    }

                    // Set references for each phone
                    if (mappedSupplier.getOrderPhones() != null) {
                        mappedSupplier.getOrderPhones().forEach(p -> p.setSupplier(mappedSupplier));
                    }

                    // Save the mapped supplier
                    Supplier savedSupplier = supplierService.save(companyId, mappedSupplier);
                    po.setSupplier(savedSupplier);
                }

                // Link purchase option back to the InventoryItem
                po.setInventoryItem(item);
                purchaseOptions.add(po);
            }

            // Attach to the item
            item.setPurchaseOptions(purchaseOptions);
        }

        // 5) Finally, save the complete InventoryItem
        return inventoryItemService.save(companyId, item);
    }
}


