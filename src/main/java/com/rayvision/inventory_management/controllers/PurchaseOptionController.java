package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.dto.PurchaseOptionCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionPartialUpdateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionResponseDTO;
import com.rayvision.inventory_management.service.PurchaseOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-options")
public class PurchaseOptionController {

    private final PurchaseOptionService purchaseOptionService;
    // used for mapping po to podto
    private final InventoryItemResponseMapper inventoryItemResponseMapper;

    @Autowired
    public PurchaseOptionController(InventoryItemResponseMapper inventoryItemResponseMapper, PurchaseOptionService purchaseOptionService) {
        this.inventoryItemResponseMapper = inventoryItemResponseMapper;
        this.purchaseOptionService = purchaseOptionService;
    }

    /**
     * 1) GET all PurchaseOptions for a given InventoryItem
     */
    @GetMapping("/inventory-items/{itemId}")
    public ResponseEntity<List<PurchaseOptionResponseDTO>> getPurchaseOptionsForItem(
            @PathVariable Long itemId
    ) {
        List<PurchaseOption> purchaseOptions =
                purchaseOptionService.getPurchaseOptions(itemId);

        List<PurchaseOptionResponseDTO> responseList = purchaseOptions.stream()
                .map(inventoryItemResponseMapper::toPurchaseOptionResponseDTO)
                .toList();

        return ResponseEntity.ok(responseList);
    }

    /**
     * 2) CREATE a new PurchaseOption
     */
    @PostMapping("/company/{companyId}/inventory-items/{itemId}")
    public ResponseEntity<PurchaseOptionResponseDTO> createPurchaseOption(
            @PathVariable Long companyId,
            @PathVariable Long itemId,
            @RequestBody PurchaseOptionCreateDTO dto
    ) {
        PurchaseOption po =
                purchaseOptionService.createPurchaseOption(companyId, itemId, dto);

        PurchaseOptionResponseDTO responseDto =
                inventoryItemResponseMapper.toPurchaseOptionResponseDTO(po);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 3) PARTIALLY UPDATE a PurchaseOption (excluding price)
     */
    @PatchMapping("/{purchaseOptionId}/company/{companyId}")
    public ResponseEntity<PurchaseOptionResponseDTO> partialUpdate(
            @PathVariable Long companyId,
            @PathVariable Long purchaseOptionId,
            @RequestBody PurchaseOptionPartialUpdateDTO dto
    ) {
        PurchaseOption updated = purchaseOptionService.partialUpdate(companyId, purchaseOptionId, dto);
        PurchaseOptionResponseDTO responseDto =
                inventoryItemResponseMapper.toPurchaseOptionResponseDTO(updated);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 4) DISABLE a PurchaseOption
     */
    @PatchMapping("/{purchaseOptionId}/company/{companyId}/disable")
    public ResponseEntity<Void> disablePurchaseOption(
            @PathVariable Long companyId,
            @PathVariable Long purchaseOptionId
    ) {
        purchaseOptionService.disablePurchaseOption(companyId, purchaseOptionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 5) UPDATE PRICE manually if no PriceHistory
     */
    @PatchMapping("/{purchaseOptionId}/company/{companyId}/price")
    public ResponseEntity<PurchaseOptionResponseDTO> updatePriceManually(
            @PathVariable Long companyId,
            @PathVariable Long purchaseOptionId,
            @RequestParam Double newPrice
    ) {
        PurchaseOption updated =
                purchaseOptionService.updatePriceManually(companyId, purchaseOptionId, newPrice);

        PurchaseOptionResponseDTO responseDto =
                inventoryItemResponseMapper.toPurchaseOptionResponseDTO(updated);

        return ResponseEntity.ok(responseDto);
    }
}
