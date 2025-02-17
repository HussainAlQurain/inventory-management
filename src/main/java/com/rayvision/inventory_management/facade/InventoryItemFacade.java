package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.mappers.impl.*;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.model.dto.SupplierDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCreateDTO;
import com.rayvision.inventory_management.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class InventoryItemFacade {

    private final CompanyService companyService;
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

    public InventoryItemFacade(CompanyService companyService, InventoryItemService inventoryItemService, CategoryService categoryService, UnitOfMeasureService unitOfMeasureService, UnitOfMeasureCategoryService unitOfMeasureCategoryService, SupplierService supplierService, CategoryMapper categoryMapper, UnitOfMeasureMapper unitOfMeasureMapper, UnitOfMeasureCategoryMapper unitOfMeasureCategoryMapper, PurchaseOptionMapper purchaseOptionMapper, SupplierMapper supplierMapper) {
        this.companyService = companyService;
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
    public InventoryItem createInventoryItem(Long companyId, InventoryItemCreateDTO dto) {
        // 1) Get the company
        Company company = companyService.findOne(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found for id: " + companyId));

        // 2) Build basic InventoryItem
        InventoryItem item = new InventoryItem();
        item.setCompany(company);
        item.setName(dto.getName());
        item.setSku(dto.getSku());
        item.setProductCode(dto.getProductCode());
        item.setDescription(dto.getDescription());
        item.setCurrentPrice(dto.getCurrentPrice());
        item.setCalories(dto.getCalories());

        // 3) Handle Item's Category
        if (dto.getCategoryId() != null) {
            Category existingCat = categoryService.getCategoryById(companyId, dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found for id: " + dto.getCategoryId()));
            item.setCategory(existingCat);

        } else if (dto.getCategory() != null) {
            Category newCat = categoryMapper.mapFrom(dto.getCategory());
            newCat.setCompany(company);
            newCat = categoryService.save(companyId, newCat);
            item.setCategory(newCat);
        }

        // 4) Handle Inventory UOM
        if (dto.getInventoryUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureService.getById(companyId, dto.getInventoryUomId())
                    .orElseThrow(() -> new RuntimeException("Inventory UOM not found for id: " + dto.getInventoryUomId()));
            item.setInventoryUom(uom);
        } else if (dto.getInventoryUom() != null) {
            UnitOfMeasureCreateDTO uomDto = dto.getInventoryUom();
            UnitOfMeasure uom = unitOfMeasureMapper.mapFrom(uomDto);

            if (uomDto.getCategoryId() != null) {
                UnitOfMeasureCategory uomCat = unitOfMeasureCategoryService.findById(uomDto.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("UOM Category not found for id: " + uomDto.getCategoryId()));
                uom.setCategory(uomCat);
            } else if (uomDto.getCategory() != null) {
                // Build new UOM Category
                UnitOfMeasureCategory newUomCat = unitOfMeasureCategoryMapper.mapFrom(uomDto.getCategory());
                // (If your UOM Category also needs a "company", do that here)
                newUomCat = unitOfMeasureCategoryService.save(companyId, newUomCat);
                uom.setCategory(newUomCat);
            }

            // Save the new UOM
            uom.setCompany(company);
            uom = unitOfMeasureService.save(companyId, uom);
            item.setInventoryUom(uom);
        }

        // 5) Handle Purchase Options
        if (dto.getPurchaseOptions() != null && !dto.getPurchaseOptions().isEmpty()) {
            Set<PurchaseOption> options = new HashSet<>();
            for (PurchaseOptionDTO poDto : dto.getPurchaseOptions()) {
                // (A) Basic PurchaseOption fields
                PurchaseOption po = purchaseOptionMapper.mapFrom(poDto);

                // (B) ordering UOM
                if (poDto.getOrderingUomId() != null) {
                    UnitOfMeasure ordUom = unitOfMeasureService.getById(companyId, poDto.getOrderingUomId())
                            .orElseThrow(() -> new RuntimeException("Ordering UOM not found for id: " + poDto.getOrderingUomId()));
                    po.setOrderingUom(ordUom);
                } else if (poDto.getOrderingUom() != null) {
                    UnitOfMeasureCreateDTO ordUomDto = poDto.getOrderingUom();
                    UnitOfMeasure ordUom = unitOfMeasureMapper.mapFrom(ordUomDto);

                    if (ordUomDto.getCategoryId() != null) {
                        UnitOfMeasureCategory ordCat = unitOfMeasureCategoryService.findById(ordUomDto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Ordering UOM Category not found for id: " + ordUomDto.getCategoryId()));
                        ordUom.setCategory(ordCat);
                    } else if (ordUomDto.getCategory() != null) {
                        UnitOfMeasureCategory newCat = unitOfMeasureCategoryMapper.mapFrom(ordUomDto.getCategory());
                        newCat = unitOfMeasureCategoryService.save(companyId, newCat);
                        ordUom.setCategory(newCat);
                    }
                    ordUom.setCompany(company);
                    ordUom = unitOfMeasureService.save(companyId, ordUom);
                    po.setOrderingUom(ordUom);
                } else {
                    // Default to the item’s own UOM
                    po.setOrderingUom(item.getInventoryUom());
                }

                // (C) Supplier reference
                if (poDto.getSupplierId() != null) {
                    // existing supplier
                    Supplier existingSupp = supplierService.getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() -> new RuntimeException("Supplier not found for id: " + poDto.getSupplierId()));
                    po.setSupplier(existingSupp);

                } else if (poDto.getSupplier() != null) {
                    // build new supplier from JSON
                    SupplierDTO suppDTO = poDto.getSupplier();

                    // 1) Create an "unsaved" supplier from DTO
                    Supplier unsavedSupplier = supplierMapper.mapFrom(suppDTO);
                    unsavedSupplier.setCompany(company);

                    // 2) Handle defaultCategory from SupplierDTO
                    if (suppDTO.getDefaultCategoryId() != null) {
                        Category existingCat = categoryService.getCategoryById(companyId, suppDTO.getDefaultCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found for id: " + suppDTO.getDefaultCategoryId()));
                        unsavedSupplier.setDefaultCategory(existingCat);

                    } else if (suppDTO.getDefaultCategory() != null) {
                        // new Category
                        Category newSuppCat = categoryMapper.mapFrom(suppDTO.getDefaultCategory());
                        newSuppCat.setCompany(company);
                        newSuppCat = categoryService.save(companyId, newSuppCat);
                        unsavedSupplier.setDefaultCategory(newSuppCat);
                    }

                    // 3) If Supplier has nested references to emails/phones, link them
                    if (unsavedSupplier.getOrderEmails() != null) {
                        unsavedSupplier.getOrderEmails().forEach(e -> e.setSupplier(unsavedSupplier));
                    }
                    if (unsavedSupplier.getOrderPhones() != null) {
                        unsavedSupplier.getOrderPhones().forEach(p -> p.setSupplier(unsavedSupplier));
                    }

                    // 4) Save the unsavedSupplier to get a persistent Supplier
                    Supplier savedSupplier = supplierService.save(companyId, unsavedSupplier);

                    // 5) Attach the saved supplier to this PurchaseOption
                    po.setSupplier(savedSupplier);
                }

                // (D) Link purchaseOption back to the item
                po.setInventoryItem(item);
                options.add(po);
            }
            item.setPurchaseOptions(options);
        }

        // 6) Finally, save the item (this should cascade to PurchaseOptions if configured)
        return inventoryItemService.save(companyId, item);
    }
}
