package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.OrderItem;
import com.rayvision.inventory_management.model.Orders;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies/{companyId}/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    // ----------------------------------------------------------------
    // GET /companies/{companyId}/purchase-orders
    //  => returns a list of OrderSummaryDTO
    //
    // Query params: ?startDate=YYYY-MM-DD & endDate=YYYY-MM-DD
    // If not provided, default 1970-01-01 to 3000-01-01
    // ----------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<OrderSummaryDTO>> getOrders(@PathVariable Long companyId,
                                                           @RequestParam(required = false) String startDate,
                                                           @RequestParam(required = false) String endDate) {
        // 1) parse or default
        LocalDate startDate2 = (startDate != null)
                ? LocalDate.parse(startDate)
                : LocalDate.of(1970, 1, 1);
        LocalDate endDate2 = (endDate != null)
                ? LocalDate.parse(endDate)
                : LocalDate.of(3000, 1, 1);

        // Convert them to LocalDateTime:
        LocalDateTime start = startDate2.atStartOfDay();
        LocalDateTime end   = endDate2.atTime(23, 59, 59);


        // 2) fetch from service
        List<Orders> ordersList = purchaseOrderService.findByCompanyAndDateRange(companyId, start, end);

        // 3) map to summary DTO
        List<OrderSummaryDTO> dtos = new ArrayList<>();
        for (Orders o : ordersList) {
            OrderSummaryDTO sumDto = new OrderSummaryDTO();
            sumDto.setId(o.getId());
            sumDto.setOrderNumber(o.getOrderNumber());
            sumDto.setSentDate(o.getSentDate());
            sumDto.setDeliveryDate(o.getDeliveryDate());
            sumDto.setStatus((o.getStatus() != null) ? o.getStatus().name() : null);
            sumDto.setComments(o.getComments());

            if (o.getBuyerLocation() != null) {
                sumDto.setBuyerLocationName(o.getBuyerLocation().getName());
            }
            if (o.getSentToSupplier() != null) {
                sumDto.setSupplierName(o.getSentToSupplier().getName());
            }
            
            // Add user information
            if (o.getCreatedByUser() != null) {
                sumDto.setCreatedByUserId(o.getCreatedByUser().getId());
                sumDto.setCreatedByUserName(o.getCreatedByUser().getUsername());
            }

            // If you want total price, sum the order items
            double sum = 0.0;
            if (o.getOrderItems() != null) {
                for (OrderItem line : o.getOrderItems()) {
                    sum += (line.getTotal() != null) ? line.getTotal() : 0.0;
                }
            }
            sumDto.setTotal(sum);

            dtos.add(sumDto);
        }

        return ResponseEntity.ok(dtos);
    }

    // ----------------------------------------------------------------
    // GET /companies/{companyId}/purchase-orders/{orderId}
    //  => returns the full detail (OrderResponseDTO)
    // ----------------------------------------------------------------
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderDetails(@PathVariable Long companyId,
                                                            @PathVariable Long orderId) {
        // In the service, you'd load the order & ensure it belongs to companyId, etc.
        Orders order = purchaseOrderService.getOrderById(companyId, orderId);

        OrderResponseDTO dto = toOrderResponseDTO(order);
        return ResponseEntity.ok(dto);
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
    // Update a draft purchase order (only works for DRAFT status)
    // ----------------------------------------------------------------
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> updateDraftOrder(
            @PathVariable Long companyId,
            @PathVariable Long orderId, 
            @RequestBody OrderUpdateDTO dto) {
        Orders updated = purchaseOrderService.updateDraftOrder(orderId, dto);
        return ResponseEntity.ok(toOrderResponseDTO(updated));
    }

    // ----------------------------------------------------------------
    // Delete a draft purchase order (only works for DRAFT status)
    // ----------------------------------------------------------------
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteDraftOrder(@PathVariable Long orderId) {
        purchaseOrderService.deleteDraftOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // Get inventory items available for ordering from a specific supplier and location (paginated)
    // ----------------------------------------------------------------
    @GetMapping("/available-items")
    public ResponseEntity<?> getAvailableInventoryItems(
            @RequestParam Long supplierId,
            @RequestParam Long locationId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String search) {
        
        // If pagination params are provided, use the paginated method
        if (page != 0 || size != 10 || !search.isEmpty()) {
            Page<InventoryItemResponseDTO> items = purchaseOrderService
                .availableItemsPaginated(supplierId, locationId, search, PageRequest.of(page, size));
                
            PageResponseDTO<InventoryItemResponseDTO> response = new PageResponseDTO<>(
                items.getContent(),
                items.getTotalElements(),
                items.getTotalPages(),
                items.getNumber(),
                items.getSize(),
                items.hasNext(),
                items.hasPrevious()
            );
            
            return ResponseEntity.ok(response);
        } else {
            // Fall back to the existing non-paginated method for backward compatibility
            List<InventoryItemResponseDTO> items = purchaseOrderService
                .getInventoryItemsBySupplierAndLocation(supplierId, locationId);
            return ResponseEntity.ok(items);
        }
    }

    // ----------------------------------------------------------------
    // GET paginated purchase orders with advanced filtering options
    // ----------------------------------------------------------------
    @GetMapping("/paginated")
    public ResponseEntity<PageResponseDTO<OrderSummaryDTO>> getPaginatedOrders(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) OrderStatus status) {
        
        // 1) parse or default dates
        LocalDateTime start = (startDate != null) 
                ? startDate.atStartOfDay() 
                : LocalDate.of(1970, 1, 1).atStartOfDay();
                
        LocalDateTime end = (endDate != null) 
                ? endDate.atTime(23, 59, 59) 
                : LocalDate.of(3000, 1, 1).atTime(23, 59, 59);
        
        // 2) Create sorting if provided
        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(direction, sortField);
        } else {
            // Default sort by creation date descending (newest first)
            sorting = Sort.by(Sort.Direction.DESC, "creationDate");
        }
        
        Pageable pageable = PageRequest.of(page, size, sorting);
        
        // 3) Fetch paginated orders with filtering
        Page<Orders> ordersPage = purchaseOrderService.searchOrders(
                companyId, supplierId, locationId, status, start, end, pageable);
        
        // 4) Convert to DTOs
        Page<OrderSummaryDTO> dtoPage = ordersPage.map(order -> {
            OrderSummaryDTO sumDto = new OrderSummaryDTO();
            sumDto.setId(order.getId());
            sumDto.setOrderNumber(order.getOrderNumber());
            sumDto.setSentDate(order.getSentDate());
            sumDto.setDeliveryDate(order.getDeliveryDate());
            sumDto.setStatus((order.getStatus() != null) ? order.getStatus().name() : null);
            sumDto.setComments(order.getComments());

            if (order.getBuyerLocation() != null) {
                sumDto.setBuyerLocationName(order.getBuyerLocation().getName());
            }
            if (order.getSentToSupplier() != null) {
                sumDto.setSupplierName(order.getSentToSupplier().getName());
            }
            
            // Add user information
            if (order.getCreatedByUser() != null) {
                sumDto.setCreatedByUserId(order.getCreatedByUser().getId());
                sumDto.setCreatedByUserName(order.getCreatedByUser().getUsername());
            }

            // Calculate total price from order items
            double sum = 0.0;
            if (order.getOrderItems() != null) {
                for (OrderItem line : order.getOrderItems()) {
                    sum += (line.getTotal() != null) ? line.getTotal() : 0.0;
                }
            }
            sumDto.setTotal(sum);
            
            return sumDto;
        });
        
        // 5) Create response with pagination metadata
        PageResponseDTO<OrderSummaryDTO> response = new PageResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.hasNext(),
                dtoPage.hasPrevious()
        );
        
        return ResponseEntity.ok(response);
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
        
        // Add user information
        if (order.getCreatedByUser() != null) {
            dto.setCreatedByUserId(order.getCreatedByUser().getId());
            dto.setCreatedByUserName(order.getCreatedByUser().getUsername());
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
