package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies/{companyId}/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Orders> createOrder(@PathVariable Long companyId,
                                              @RequestBody OrderCreateDTO dto) {
        Orders created = purchaseOrderService.createOrder(companyId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // SEND
    @PatchMapping("/{orderId}/send")
    public ResponseEntity<Orders> sendOrder(@PathVariable Long companyId,
                                            @PathVariable Long orderId,
                                            @RequestParam(required=false) String comments) {
        Orders updated = purchaseOrderService.sendOrder(orderId, comments);
        return ResponseEntity.ok(updated);
    }

    // RECEIVE
    @PatchMapping("/{orderId}/receive")
    public ResponseEntity<Orders> receiveOrder(@PathVariable Long companyId,
                                               @PathVariable Long orderId) {
        Orders updated = purchaseOrderService.receiveOrder(orderId);
        return ResponseEntity.ok(updated);
    }

}
