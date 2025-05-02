package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.NoOrderInvoiceDTO;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderUpdateDTO;
import com.rayvision.inventory_management.model.dto.ReceiveLineDTO;
import com.rayvision.inventory_management.model.dto.InventoryItemResponseDTO;
import com.rayvision.inventory_management.service.impl.AutoOrderScheduledService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    
    /**
     * Find orders with pagination and advanced filtering options
     *
     * @param companyId The company ID
     * @param supplierId Optional supplier ID to filter by
     * @param locationId Optional location ID to filter by
     * @param status Optional order status to filter by
     * @param start Start date for filtering
     * @param end End date for filtering
     * @param pageable Pagination and sorting information
     * @return Page of orders matching the criteria
     */
    Page<Orders> searchOrders(Long companyId, Long supplierId, Long locationId, 
                             OrderStatus status, LocalDateTime start, LocalDateTime end,
                             Pageable pageable);

    /**
     * Get paginated inventory items available for ordering from a specific supplier and location
     *
     * @param supplierId The supplier ID
     * @param locationId The location ID
     * @param searchTerm Optional search term to filter items
     * @param pageable Pagination information
     * @return Page of inventory items available for ordering
     */
    Page<InventoryItemResponseDTO> availableItemsPaginated(Long supplierId, Long locationId, 
                                                          String searchTerm, Pageable pageable);
}
