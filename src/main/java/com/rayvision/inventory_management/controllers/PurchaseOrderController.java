package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.OrderItem;
import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@PathVariable Long companyId,
                                                        @RequestBody OrderCreateDTO dto) {
        Orders created = purchaseOrderService.createOrder(companyId, dto);

        // Convert to a DTO
        OrderResponseDTO resultDto = toOrderResponseDTO(created);

        return new ResponseEntity<>(resultDto, HttpStatus.CREATED);
    }

    // Similarly for sendOrder, receiveOrder, etc.
    // e.g.:
    @PatchMapping("/{orderId}/send")
    public ResponseEntity<OrderResponseDTO> sendOrder(
            @PathVariable Long companyId,
            @PathVariable Long orderId,
            @RequestParam(required=false) String comments) {
        Orders updated = purchaseOrderService.sendOrder(orderId, comments);
        return ResponseEntity.ok(toOrderResponseDTO(updated));
    }

    @PatchMapping("/{orderId}/receive")
    public ResponseEntity<OrderResponseDTO> receiveOrder(
            @PathVariable Long companyId,
            @PathVariable Long orderId,
            @RequestBody List<ReceiveLineDTO> lines) {
        Orders updated = purchaseOrderService.receiveOrder(orderId, lines);
        return ResponseEntity.ok(toOrderResponseDTO(updated));
    }

    // etc.

    // ---------------------
    // MAPPER / converter
    // ---------------------
    private OrderResponseDTO toOrderResponseDTO(Orders order) {
        if (order == null) {
            return null;
        }
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCreationDate(order.getCreationDate());
        dto.setSentDate(order.getSentDate());
        dto.setDeliveryDate(order.getDeliveryDate());
        dto.setStatus((order.getStatus() != null) ? order.getStatus().name() : null);
        dto.setComments(order.getComments());

        if (order.getBuyerLocation() != null) {
            dto.setBuyerLocationId(order.getBuyerLocation().getId());
            dto.setBuyerLocationName(order.getBuyerLocation().getName());
        }
        if (order.getSentToSupplier() != null) {
            dto.setSupplierId(order.getSentToSupplier().getId());
            dto.setSupplierName(order.getSentToSupplier().getName());
        }

        // lines
        List<OrderItemResponseDTO> lineDtos = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem oi : order.getOrderItems()) {
                OrderItemResponseDTO lidto = new OrderItemResponseDTO();
                lidto.setOrderItemId(oi.getId());
                if (oi.getInventoryItem() != null) {
                    lidto.setInventoryItemId(oi.getInventoryItem().getId());
                    lidto.setInventoryItemName(oi.getInventoryItem().getName());
                }
                lidto.setQuantity(oi.getQuantity());
                lidto.setPrice(oi.getPrice());
                lidto.setTotal(oi.getTotal());

                // UOM name:
                if (oi.getUnitOfOrdering() != null) {
                    lidto.setUomName(oi.getUnitOfOrdering().getName());
                }

                lineDtos.add(lidto);
            }
        }
        dto.setItems(lineDtos);

        return dto;
    }

}
