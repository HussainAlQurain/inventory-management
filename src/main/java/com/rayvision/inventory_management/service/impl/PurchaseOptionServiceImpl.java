package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.PurchaseOptionCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionPartialUpdateDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.PurchaseOptionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseOptionServiceImpl implements PurchaseOptionService {
    private final PurchaseOptionRepository purchaseOptionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public PurchaseOptionServiceImpl(
            PurchaseOptionRepository purchaseOptionRepository,
            InventoryItemRepository inventoryItemRepository,
            SupplierRepository supplierRepository,
            UnitOfMeasureRepository unitOfMeasureRepository,
            CompanyRepository companyRepository
    ) {
        this.purchaseOptionRepository = purchaseOptionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.supplierRepository = supplierRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    @Override
    public PurchaseOption createPurchaseOption(Long companyId, Long inventoryItemId, PurchaseOptionCreateDTO dto) {
        // 1) Lookup InventoryItem to link
        InventoryItem item = inventoryItemRepository.findByCompanyIdAndId(companyId, inventoryItemId)
                .orElseThrow(() -> new RuntimeException(
                        "InventoryItem not found with id " + inventoryItemId + " for company " + companyId
                ));

        // 2) Build a new PurchaseOption
        PurchaseOption po = new PurchaseOption();
        po.setInventoryItem(item);
        po.setPrice(dto.getPrice());
        po.setTaxRate(dto.getTaxRate());
        po.setInnerPackQuantity(dto.getInnerPackQuantity());
        po.setPacksPerCase(dto.getPacksPerCase());
        po.setMinOrderQuantity(dto.getMinOrderQuantity());
        po.setMainPurchaseOption(dto.isMainPurchaseOption());
        po.setOrderingEnabled(dto.isOrderingEnabled());
        po.setSupplierProductCode(dto.getSupplierProductCode());
        po.setNickname(dto.getNickname());
        po.setScanBarcode(dto.getScanBarcode());

        // (Optional) Link the supplier if present
        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findByCompanyIdAndId(companyId, dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException(
                            "Supplier not found with id " + dto.getSupplierId() + " for company " + companyId
                    ));
            po.setSupplier(supplier);
        }

        // (Optional) Link the ordering UOM if present
        if (dto.getOrderingUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findByCompanyIdAndId(companyId, dto.getOrderingUomId())
                    .orElseThrow(() -> new RuntimeException(
                            "Ordering UOM not found with id " + dto.getOrderingUomId() + " for company " + companyId
                    ));
            po.setOrderingUom(uom);
        } else {
            // default to the InventoryItem's UOM if you want:
            po.setOrderingUom(item.getInventoryUom());
        }

        // 3) Save
        return purchaseOptionRepository.save(po);
    }

    @Override
    public PurchaseOption partialUpdate(Long companyId, Long purchaseOptionId, PurchaseOptionPartialUpdateDTO dto) {
        // 1) Find existing PO
        PurchaseOption existing = purchaseOptionRepository.findById(purchaseOptionId)
                .orElseThrow(() -> new RuntimeException("PurchaseOption not found with id " + purchaseOptionId));

        // Update only if non-null
        if (dto.getTaxRate() != null) {
            existing.setTaxRate(dto.getTaxRate());
        }
        if (dto.getInnerPackQuantity() != null) {
            existing.setInnerPackQuantity(dto.getInnerPackQuantity());
        }
        if (dto.getPacksPerCase() != null) {
            existing.setPacksPerCase(dto.getPacksPerCase());
        }
        if (dto.getMinOrderQuantity() != null) {
            existing.setMinOrderQuantity(dto.getMinOrderQuantity());
        }
        if (dto.getMainPurchaseOption() != null) {  // Only update if provided
            if(dto.getMainPurchaseOption())
            {
                this.setAsMain(companyId, purchaseOptionId);
            }
        }
        if (dto.getOrderingEnabled() != null) {  // Only update if provided
            existing.setOrderingEnabled(dto.getOrderingEnabled());
        }
        if (dto.getSupplierProductCode() != null) {
            existing.setSupplierProductCode(dto.getSupplierProductCode());
        }
        if (dto.getNickname() != null) {
            existing.setNickname(dto.getNickname());
        }
        if (dto.getScanBarcode() != null) {
            existing.setScanBarcode(dto.getScanBarcode());
        }

        // If you allow changing supplier:
        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findByCompanyIdAndId(companyId, dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found with id " + dto.getSupplierId() + " for company " + companyId));
            existing.setSupplier(supplier);
        }

        // If you allow changing ordering UOM:
        if (dto.getOrderingUomId() != null) {
            UnitOfMeasure uom = unitOfMeasureRepository.findByCompanyIdAndId(companyId, dto.getOrderingUomId())
                    .orElseThrow(() -> new RuntimeException("Ordering UOM not found with id " + dto.getOrderingUomId() + " for company " + companyId));
            existing.setOrderingUom(uom);
        }

        // 3) Save and return the updated PO
        return purchaseOptionRepository.save(existing);
    }


    @Override
    public void disablePurchaseOption(Long companyId, Long purchaseOptionId) {
        PurchaseOption existing = purchaseOptionRepository.findById(purchaseOptionId)
                .orElseThrow(() -> new RuntimeException(
                        "PurchaseOption not found with id " + purchaseOptionId
                ));

        // check if belongs to same company
        Long itemCompanyId = existing.getInventoryItem().getCompany().getId();
        if (!itemCompanyId.equals(companyId)) {
            throw new RuntimeException("PurchaseOption does not belong to company " + companyId);
        }

        // "Disable" -> set orderingEnabled to false (or any other approach)
        existing.setOrderingEnabled(false);
        purchaseOptionRepository.save(existing);
    }

    @Override
    public PurchaseOption updatePriceManually(Long companyId, Long purchaseOptionId, Double newPrice) {
        // 1) Find existing PO
        PurchaseOption existing = purchaseOptionRepository.findById(purchaseOptionId)
                .orElseThrow(() -> new RuntimeException(
                        "PurchaseOption not found with id " + purchaseOptionId
                ));

        // 2) Ensure same company
        Long itemCompanyId = existing.getInventoryItem().getCompany().getId();
        if (!itemCompanyId.equals(companyId)) {
            throw new RuntimeException("PurchaseOption does not belong to company " + companyId);
        }

        // 3) Check if price history is empty
        // If you store purchaseOption.getPriceHistories() as a Set<PurchaseOptionPriceHistory>:
        if (existing.getPriceHistories() != null && !existing.getPriceHistories().isEmpty()) {
            throw new RuntimeException("Cannot manually update price once price history exists.");
        }

        // 4) Update price
        existing.setPrice(newPrice);

        // 5) Save
        return purchaseOptionRepository.save(existing);
    }

    @Override
    public List<PurchaseOption> getPurchaseOptions(Long inventoryItemId) {
        return purchaseOptionRepository.findByInventoryItemId(inventoryItemId);
    }

    @Override
    public void deletePurchaseOption(Long companyId, Long purchaseOptionId) {
        PurchaseOption existing = purchaseOptionRepository.findById(purchaseOptionId)
                .orElseThrow(() -> new RuntimeException("PurchaseOption not found: " + purchaseOptionId));

        // optional check belongs to same company
        if (!existing.getInventoryItem().getCompany().getId().equals(companyId)) {
            throw new RuntimeException("PurchaseOption does not belong to this company");
        }

        purchaseOptionRepository.delete(existing);

    }

    @Override
    public void setAsMain(Long companyId, Long purchaseOptionId) {
        // Fetch the target purchase option
        PurchaseOption target = purchaseOptionRepository.findById(purchaseOptionId)
                .orElseThrow(() -> new RuntimeException("PurchaseOption not found: " + purchaseOptionId));

        // Get the inventory item ID for this purchase option
        Long itemId = target.getInventoryItem().getId();

        // Retrieve all purchase options for this inventory item
        List<PurchaseOption> purchaseOptions = purchaseOptionRepository.findByInventoryItemId(itemId);

        // Loop over them and update only the mainPurchaseOption flag:
        for (PurchaseOption po : purchaseOptions) {
            // Set the flag to true only for the target purchase option, false otherwise.
            boolean shouldBeMain = po.getId().equals(purchaseOptionId);
            po.setMainPurchaseOption(shouldBeMain);
            // Save the purchase option without modifying its orderingEnabled property.
            purchaseOptionRepository.save(po);
        }
    }


}
