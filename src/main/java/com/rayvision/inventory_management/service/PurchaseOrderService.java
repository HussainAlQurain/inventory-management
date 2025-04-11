package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.NoOrderInvoiceDTO;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderUpdateDTO;
import com.rayvision.inventory_management.model.dto.ReceiveLineDTO;
import com.rayvision.inventory_management.model.dto.InventoryItemResponseDTO;
import com.rayvision.inventory_management.service.impl.AutoOrderScheduledService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PurchaseOrderService {
    Orders createOrder(Long companyId, OrderCreateDTO dto);
    Orders sendOrder(Long orderId, String comments);
    Orders receiveOrder(Long orderId, List<ReceiveLineDTO> lines, boolean updateOptionPrice);
    Orders receiveWithoutOrder(Long companyId, NoOrderInvoiceDTO dto);
    List<Orders> fillToPar(Long locationId, Long userId);
    Orders findDraftOrderForSupplierAndLocation(Long supplierId, Long locationId);
    Orders updateDraftOrderWithShortages(Orders draft, List<AutoOrderScheduledService.ShortageLine> lines, String comment);
    Map<Long, Double> calculateInTransitQuantitiesByLocation(Long locationId);
    List<Orders> findByCompanyAndDateRange(Long companyId, LocalDateTime start, LocalDateTime end);
    Orders getOrderById(Long companyId, Long orderId);
    
    // New methods
    Orders updateDraftOrder(Long orderId, OrderUpdateDTO dto);
    void deleteDraftOrder(Long orderId);
    List<InventoryItemResponseDTO> getInventoryItemsBySupplierAndLocation(Long supplierId, Long locationId);
}
