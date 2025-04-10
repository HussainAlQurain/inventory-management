package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.NoOrderInvoiceDTO;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.ReceiveLineDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseOrderService {
    Orders createOrder(Long companyId, OrderCreateDTO dto);
    Orders sendOrder(Long orderId, String comments);
    Orders receiveOrder(Long orderId, List<ReceiveLineDTO> lines, boolean updateOptionPrice);
    Orders receiveWithoutOrder(Long companyId, NoOrderInvoiceDTO dto);
    List<Orders> fillToPar(Long locationId, Long userId);
    // or partialReceive
    // maybe more methods as needed
    List<Orders> findByCompanyAndDateRange(Long companyId, LocalDateTime start, LocalDateTime end);

    // ...
    Orders getOrderById(Long companyId, Long orderId);

}
