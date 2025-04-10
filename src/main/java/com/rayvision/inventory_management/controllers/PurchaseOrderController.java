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

    // ----------------------------------------------------------------
    // 1) Create Order (POST)
    //    Request: OrderCreateDTO
    //    Response: OrderResponseDTO
    // ----------------------------------------------------------------
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@PathVariable Long companyId,
                                                        @RequestBody OrderCreateDTO dto) {
        Orders created = purchaseOrderService.createOrder(companyId, dto);
        OrderResponseDTO resultDto = toOrderResponseDTO(created);
        return new ResponseEntity<>(resultDto, HttpStatus.CREATED);
    }

    // ----------------------------------------------------------------
    // 2) Send Order (PATCH /{orderId}/send)
    //    Optional query param "comments"
    //    Return the updated OrderResponseDTO
    // ----------------------------------------------------------------
    @PatchMapping("/{orderId}/send")
    public ResponseEntity<OrderResponseDTO> sendOrder(@PathVariable Long companyId,
                                                      @PathVariable Long orderId,
                                                      @RequestParam(required = false) String comments) {
        Orders updated = purchaseOrderService.sendOrder(orderId, comments);
        return ResponseEntity.ok(toOrderResponseDTO(updated));
    }

    // ----------------------------------------------------------------
    // 3) Receive Order (PATCH /{orderId}/receive)
    //    Body: List<ReceiveLineDTO> => partial or full receiving lines
    //    Query param: updateOptionPrice? (true/false)
    // ----------------------------------------------------------------
    @PatchMapping("/{orderId}/receive")
    public ResponseEntity<OrderResponseDTO> receiveOrder(@PathVariable Long companyId,
                                                         @PathVariable Long orderId,
                                                         @RequestBody List<ReceiveLineDTO> lines,
                                                         @RequestParam(defaultValue = "false") boolean updateOptionPrice) {
        // Let the service handle the updateOptionPrice logic
        Orders updated = purchaseOrderService.receiveOrder(orderId, lines, updateOptionPrice);
        return ResponseEntity.ok(toOrderResponseDTO(updated));
    }

    // ----------------------------------------------------------------
    // 4) No-Order Invoice (POST /no-order-invoice)
    //    Body: NoOrderInvoiceDTO
    // ----------------------------------------------------------------
    @PostMapping("/no-order-invoice")
    public ResponseEntity<OrderResponseDTO> receiveWithoutOrder(@PathVariable Long companyId,
                                                                @RequestBody NoOrderInvoiceDTO dto) {
        Orders result = purchaseOrderService.receiveWithoutOrder(companyId, dto);
        return new ResponseEntity<>(toOrderResponseDTO(result), HttpStatus.CREATED);
    }

    // ----------------------------------------------------------------
    // 5) Fill to PAR (POST /fill-to-par?locationId=xxx&userId=yyy)
    //    Return a list of newly created DRAFT orders
    // ----------------------------------------------------------------
    @PostMapping("/fill-to-par")
    public ResponseEntity<List<OrderResponseDTO>> fillToPar(@PathVariable Long companyId,
                                                            @RequestParam Long locationId,
                                                            @RequestParam Long userId) {
        List<Orders> createdDrafts = purchaseOrderService.fillToPar(locationId, userId);

        // convert each to DTO
        List<OrderResponseDTO> result = new ArrayList<>();
        for (Orders o : createdDrafts) {
            result.add(toOrderResponseDTO(o));
        }
        return ResponseEntity.ok(result);
    }

    // ----------------------------------------------------------------
    // Internal Mapper from Orders -> OrderResponseDTO
    // ----------------------------------------------------------------
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

        // Build line DTOs
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
