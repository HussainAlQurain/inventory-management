package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;

public interface PurchaseOrderService {
    Orders createOrder(Long companyId, OrderCreateDTO dto);
    Orders sendOrder(Long orderId, String comments);
    Orders receiveOrder(Long orderId);  // fully receive
    // or partialReceive
    // maybe more methods as needed

}
