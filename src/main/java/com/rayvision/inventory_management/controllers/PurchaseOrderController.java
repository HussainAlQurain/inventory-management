package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.NoOrderInvoiceDTO;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.ReceiveLineDTO;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    /**
     * CREATE a new “order” (in DRAFT).
     * The user’s JSON includes:
     *  - supplierId (the entire order belongs to 1 supplier)
     *  - buyerLocationId
     *  - createdByUserId
     *  - items: [ {inventoryItemId, quantity, price?, unitOfMeasureId?}, ... ]
     *
     * The service will:
     *   1) Validate item’s PurchaseOption that matches the supplier (if user didn’t explicitly set price).
     *   2) Default the price from the purchase option if itemDto.price==null
     *   3) Default the UOM from the purchase option if itemDto.unitOfMeasureId==null
     */
    @PostMapping
    public ResponseEntity<Orders> createOrder(@PathVariable Long companyId,
                                              @RequestBody OrderCreateDTO dto) {
        Orders created = purchaseOrderService.createOrder(companyId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * SEND an existing DRAFT order to the supplier (status -> SENT).
     * Optionally pass a “comments” param in the querystring (or body).
     */
    @PatchMapping("/{orderId}/send")
    public ResponseEntity<Orders> sendOrder(@PathVariable Long companyId,
                                            @PathVariable Long orderId,
                                            @RequestParam(required=false) String comments) {
        Orders updated = purchaseOrderService.sendOrder(orderId, comments);
        return ResponseEntity.ok(updated);
    }

    /**
     * RECEIVE an order (partial or full).
     * We pass a JSON array of line overrides: {orderItemId, receivedQty, finalPrice}.
     * If finalQty < original => partial receiving. If finalPrice differs => cost override.
     */
    @PatchMapping("/{orderId}/receive")
    public ResponseEntity<Orders> receiveOrder(@PathVariable Long companyId,
                                               @PathVariable Long orderId,
                                               @RequestBody List<ReceiveLineDTO> lines) {
        Orders updated = purchaseOrderService.receiveOrder(orderId, lines);
        return ResponseEntity.ok(updated);
    }

    /**
     * NO-ORDER invoice: direct receiving of items.
     * Creates an order in COMPLETED status + stock transactions.
     * JSON:
     *  {
     *    "locationId": 12,
     *    "userId": 999,
     *    "supplierId": 123, (optional)
     *    "comments": "We forgot to do a PO",
     *    "lines": [
     *       {"itemId":123, "uomId":999, "quantity": 10.0, "price":12.0}, ...
     *    ]
     *  }
     */
    @PostMapping("/no-order-invoice")
    public ResponseEntity<Orders> receiveWithoutOrder(@PathVariable Long companyId,
                                                      @RequestBody NoOrderInvoiceDTO dto) {
        Orders result = purchaseOrderService.receiveWithoutOrder(companyId, dto);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /**
     * Fill to PAR for a location & user -> auto-create DRAFT orders for items below par.
     * For example, the user calls:
     *   POST /companies/1/purchase-orders/fill-to-par?locationId=10&userId=999
     * and we return a list of newly created DRAFT orders.
     */
    @PostMapping("/fill-to-par")
    public ResponseEntity<List<Orders>> fillToPar(@PathVariable Long companyId,
                                                  @RequestParam Long locationId,
                                                  @RequestParam Long userId) {
        List<Orders> result = purchaseOrderService.fillToPar(locationId, userId);
        return ResponseEntity.ok(result);
    }

}
