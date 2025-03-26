package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.exceptions.InvalidOperationException;
import com.rayvision.inventory_management.exceptions.ResourceNotFoundException;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderItemDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final OrderRepository ordersRepository;
    private final LocationRepository locationRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final StockTransactionService stockTransactionService;

    public PurchaseOrderServiceImpl(
            OrderRepository ordersRepository,
            LocationRepository locationRepository,
            SupplierRepository supplierRepository,
            UserRepository userRepository,
            InventoryItemRepository inventoryItemRepository,
            UnitOfMeasureRepository uomRepository,
            StockTransactionService stockTransactionService
    ) {
        this.ordersRepository = ordersRepository;
        this.locationRepository = locationRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.uomRepository = uomRepository;
        this.stockTransactionService = stockTransactionService;
    }

    @Override
    public Orders createOrder(Long companyId, OrderCreateDTO dto) {
        // 1) Validate the location, supplier, user
        Location buyerLocation = locationRepository.findById(dto.getBuyerLocationId())
                .orElseThrow(() -> new RuntimeException("Buyer location not found"));
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        Users user = userRepository.findById(dto.getCreatedByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) Build Orders
        Orders order = new Orders();
        order.setCompany(buyerLocation.getCompany()); // or find by companyId if needed
        order.setBuyerLocation(buyerLocation);
        order.setSentToSupplier(supplier);
        order.setCreatedByUser(user);
        order.setCreationDate(LocalDateTime.now());
        order.setStatus(OrderStatus.DRAFT);
        order.setComments(dto.getComments());
        order.setOrderNumber("PO-" + System.currentTimeMillis()); // just example

        // 3) Build order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO itemDto : dto.getItems()) {
            InventoryItem invItem = inventoryItemRepository.findById(itemDto.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem not found: " + itemDto.getInventoryItemId()));
            UnitOfMeasure uom = null;
            if (itemDto.getUnitOfMeasureId() != null) {
                uom = uomRepository.findById(itemDto.getUnitOfMeasureId())
                        .orElseThrow(() -> new RuntimeException("UOM not found: " + itemDto.getUnitOfMeasureId()));
            } else {
                // fallback
                uom = invItem.getInventoryUom();
            }

            OrderItem oi = new OrderItem();
            oi.setOrders(order);
            oi.setInventoryItem(invItem);
            oi.setUnitOfOrdering(uom);
            oi.setQuantity(itemDto.getQuantity());
            oi.setPrice(itemDto.getPrice());
            oi.setTotal(itemDto.getQuantity() * itemDto.getPrice());
            // extendedQuantity is optional
            oi.setExtendedQuantity(itemDto.getQuantity());
            orderItems.add(oi);
        }
        order.setOrderItems(orderItems);

        return ordersRepository.save(order);
    }

    @Override
    public Orders sendOrder(Long orderId, String comments) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new InvalidOperationException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Order must be in DRAFT to send");
        }
        order.setStatus(OrderStatus.SENT);
        order.setComments(comments);
        // maybe set sentDate
        order.setSentDate(java.time.LocalDate.now());
        return ordersRepository.save(order);
    }

    /**
     * Fully receive the order. This is where we do stockTransactionService.recordPurchase for each item
     */
    @Override
    public Orders receiveOrder(Long orderId) {
        Orders order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new InvalidOperationException("Only draft orders can be sent");
        }

        // Mark as RECEIVED
        order.setStatus(OrderStatus.COMPLETED);
        order.setDeliveryDate(java.time.LocalDate.now());

        // For each item, record a PURCHASE transaction
        // referencing the order's ID as sourceReferenceId
        for (OrderItem line : order.getOrderItems()) {
            Double qty = line.getQuantity();
            Double cost = line.getTotal(); // or line.getQuantity() * line.getPrice()
            stockTransactionService.recordPurchase(
                    order.getBuyerLocation(),
                    line.getInventoryItem(),
                    qty,
                    cost,
                    order.getId(),
                    order.getDeliveryDate()  // date of receiving
            );
        }

        return ordersRepository.save(order);
    }

    private Orders getOrderById(Long orderId) {
        return ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

}
