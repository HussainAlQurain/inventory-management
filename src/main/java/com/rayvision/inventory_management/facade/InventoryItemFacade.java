package com.rayvision.inventory_management.facade;

import com.rayvision.inventory_management.mappers.SupplierMapper;  // <--- The new mapper
import com.rayvision.inventory_management.mappers.impl.CategoryMapper;
import com.rayvision.inventory_management.mappers.impl.PurchaseOptionMapper;
import com.rayvision.inventory_management.mappers.impl.UnitOfMeasureCategoryMapper;
import com.rayvision.inventory_management.mappers.impl.UnitOfMeasureMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.model.dto.SupplierCreateDTO;   // used if your PurchaseOptionDTO has it
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
    private final SupplierMapper supplierMapper; // <--- The new annotation-based supplier mapper

    public InventoryItemFacade(CompanyService companyService,
                               InventoryItemService inventoryItemService,
                               CategoryService categoryService,
                               UnitOfMeasureService unitOfMeasureService,
                               UnitOfMeasureCategoryService unitOfMeasureCategoryService,
                               SupplierService supplierService,
                               CategoryMapper categoryMapper,
                               UnitOfMeasureMapper unitOfMeasureMapper,
                               UnitOfMeasureCategoryMapper unitOfMeasureCategoryMapper,
                               PurchaseOptionMapper purchaseOptionMapper,
                               SupplierMapper supplierMapper) {
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
        // (1) Fetch Company
        Company company = companyService.findOne(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found for id: " + companyId));

        // (2) Build new InventoryItem
        InventoryItem item = new InventoryItem();
        item.setCompany(company);
        item.setName(dto.getName());
        item.setSku(dto.getSku());
        item.setProductCode(dto.getProductCode());
        item.setDescription(dto.getDescription());
        item.setCurrentPrice(dto.getCurrentPrice());
        item.setCalories(dto.getCalories());

        // (3) Category
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

        // (4) Inventory UOM
        if (dto.getInventoryUomId() != null) {
            UnitOfMeasure existingUom = unitOfMeasureService.getById(companyId, dto.getInventoryUomId())
                    .orElseThrow(() -> new RuntimeException("Inventory UOM not found: " + dto.getInventoryUomId()));
            item.setInventoryUom(existingUom);
        } else if (dto.getInventoryUom() != null) {
            UnitOfMeasureCreateDTO uomDto = dto.getInventoryUom();
            UnitOfMeasure newUom = unitOfMeasureMapper.mapFrom(uomDto);

            if (uomDto.getCategoryId() != null) {
                UnitOfMeasureCategory existingUomCat = unitOfMeasureCategoryService.findById(uomDto.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("UOM Category not found: " + uomDto.getCategoryId()));
                newUom.setCategory(existingUomCat);
            } else if (uomDto.getCategory() != null) {
                UnitOfMeasureCategory newUomCat = unitOfMeasureCategoryMapper.mapFrom(uomDto.getCategory());
                newUomCat = unitOfMeasureCategoryService.save(companyId, newUomCat);
                newUom.setCategory(newUomCat);
            }
            newUom.setCompany(company);
            newUom = unitOfMeasureService.save(companyId, newUom);
            item.setInventoryUom(newUom);
        }

        // (5) Purchase Options
        if (dto.getPurchaseOptions() != null && !dto.getPurchaseOptions().isEmpty()) {
            Set<PurchaseOption> options = new HashSet<>();

            for (PurchaseOptionDTO poDto : dto.getPurchaseOptions()) {
                // (A) Basic fields
                PurchaseOption po = purchaseOptionMapper.mapFrom(poDto);

                // (B) Ordering UOM
                if (poDto.getOrderingUomId() != null) {
                    UnitOfMeasure ordUom = unitOfMeasureService.getById(companyId, poDto.getOrderingUomId())
                            .orElseThrow(() -> new RuntimeException("Ordering UOM not found: " + poDto.getOrderingUomId()));
                    po.setOrderingUom(ordUom);
                } else if (poDto.getOrderingUom() != null) {
                    // create new UOM
                    UnitOfMeasureCreateDTO ordUomDto = poDto.getOrderingUom();
                    UnitOfMeasure ordUom = unitOfMeasureMapper.mapFrom(ordUomDto);
                    if (ordUomDto.getCategoryId() != null) {
                        UnitOfMeasureCategory ordCat = unitOfMeasureCategoryService.findById(ordUomDto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("UOM category not found: " + ordUomDto.getCategoryId()));
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
                    // default to the item’s own UOM
                    po.setOrderingUom(item.getInventoryUom());
                }

                // (C) Supplier reference
                if (poDto.getSupplierId() != null) {
                    // link existing supplier
                    Supplier existingSupplier = supplierService.getSupplierById(companyId, poDto.getSupplierId())
                            .orElseThrow(() -> new RuntimeException("Supplier not found for id: " + poDto.getSupplierId()));
                    po.setSupplier(existingSupplier);
                }
                else if (poDto.getSupplier() != null) {
                    // Instead of the old ModelMapper-based approach, we use the new SupplierMapper
                    // that expects a SupplierCreateDTO.
                    // => So ensure PurchaseOptionDTO.supplier is actually a SupplierCreateDTO type
                    //    if you want nested phones/emails/authorizedBuyerIds.

                    // 1) Convert the nested supplier from SupplierCreateDTO -> entity
                    //    (If your PurchaseOptionDTO uses exactly SupplierCreateDTO, we can do):
                    SupplierCreateDTO newSuppDto = poDto.getSupplier();

                    Supplier unsavedSupplier = supplierMapper.fromSupplierCreateDTO(newSuppDto);
                    // do not set unsavedSupplier.setCompany(...) because the supplierService.save will do it

                    // 2) If user provided defaultCategory inline object, do it here or rely on the
                    //    'defaultCategoryId' from newSuppDto. The 'supplierService.save(...)'
                    //    fetches the actual category if the ID is set,
                    //    but if you want to create a brand-new category, do that here.
                    //    (Similar to your old logic.)

                    if (newSuppDto.getDefaultCategoryId() != null) {
                        Category existingCat = categoryService.getCategoryById(companyId, newSuppDto.getDefaultCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found for id: " + newSuppDto.getDefaultCategoryId()));
                        unsavedSupplier.setDefaultCategory(existingCat);
                    }
                    // If your DTO has defaultCategory object itself, you can handle it similarly.

                    // 3) Now call supplierService.save(...) to do phones/emails bridging, authorizedBuyerIds bridging, etc.
                    Supplier savedSupplier = supplierService.save(companyId, unsavedSupplier);

                    // 4) set purchaseOption's supplier
                    po.setSupplier(savedSupplier);
                }

                // (D) link purchaseOption -> item
                po.setInventoryItem(item);
                options.add(po);
            }
            item.setPurchaseOptions(options);
        }

        // (6) Finally save InventoryItem
        return inventoryItemService.save(companyId, item);
    }
}
