package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.exceptions.InvalidOperationException;
import com.rayvision.inventory_management.exceptions.ResourceNotFoundException;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final EmailSenderService emailSenderService;

    public PurchaseOrderServiceImpl(
            OrderRepository ordersRepository,
            LocationRepository locationRepository,
            SupplierRepository supplierRepository,
            UserRepository userRepository,
            InventoryItemRepository inventoryItemRepository,
            UnitOfMeasureRepository uomRepository,
            StockTransactionService stockTransactionService,
            EmailSenderService emailSenderService
    ) {
        this.ordersRepository = ordersRepository;
        this.locationRepository = locationRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.uomRepository = uomRepository;
        this.stockTransactionService = stockTransactionService;
        this.emailSenderService = emailSenderService;
    }

    // -------------------------------------------------------------
    // 1) CREATE ORDER (acts as “cart” when status=DRAFT)
    // -------------------------------------------------------------
    @Override
    public Orders createOrder(Long companyId, OrderCreateDTO dto) {
        // 1) Validate location, supplier, user
        Location buyerLocation = locationRepository.findById(dto.getBuyerLocationId())
                .orElseThrow(() -> new RuntimeException(
                        "Buyer location not found: " + dto.getBuyerLocationId()));
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new RuntimeException(
                        "Supplier not found: " + dto.getSupplierId()));
        Users user = userRepository.findById(dto.getCreatedByUserId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + dto.getCreatedByUserId()));

        // 2) Build the Order
        Orders order = new Orders();
        order.setCompany(buyerLocation.getCompany());
        order.setBuyerLocation(buyerLocation);
        order.setSentByLocation(buyerLocation);  // as requested
        order.setSentToSupplier(supplier);
        order.setCreatedByUser(user);
        order.setCreationDate(LocalDateTime.now());
        order.setStatus(OrderStatus.DRAFT);
        order.setComments(dto.getComments());
        order.setOrderNumber("PO-" + System.currentTimeMillis());

        // 3) Build items (ignoring user-supplied price/UOM)
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO itemDto : dto.getItems()) {
            // find the InventoryItem
            InventoryItem invItem = inventoryItemRepository
                    .findById(itemDto.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem not found: " + itemDto.getInventoryItemId()));

            // find a purchase option for this item that references the same supplier
            // and also orderingEnabled = true
            Set<PurchaseOption> matchingOptions = new HashSet<>();
            for (PurchaseOption po : invItem.getPurchaseOptions()) {
                if (po.getSupplier() != null
                        && po.getSupplier().getId().equals(supplier.getId())
                        && po.isOrderingEnabled())
                {
                    matchingOptions.add(po);
                }
            }
            if (matchingOptions.isEmpty()) {
                throw new RuntimeException(
                        "No *enabled* purchase option found for item '" + invItem.getName()
                                + "' with supplier '" + supplier.getName()
                                + "'. Please create/enable a purchase option first."
                );
            }

            // pick the "main" if possible, else pick the first
            PurchaseOption chosenOption = matchingOptions.stream()
                    .filter(PurchaseOption::isMainPurchaseOption)
                    .findFirst()
                    .orElse(matchingOptions.iterator().next());

            // We'll use chosenOption.getPrice() as the price
            Double finalPrice = (chosenOption.getPrice() != null) ? chosenOption.getPrice() : 0.0;
            Double qty = (itemDto.getQuantity() != null) ? itemDto.getQuantity() : 0.0;

            // We'll use chosenOption.getOrderingUom() as the UOM if not null,
            // fallback to item’s inventoryUom otherwise
            UnitOfMeasure finalUom = (chosenOption.getOrderingUom() != null)
                    ? chosenOption.getOrderingUom()
                    : invItem.getInventoryUom();

            // create an OrderItem
            OrderItem oi = new OrderItem();
            oi.setOrders(order);
            oi.setInventoryItem(invItem);
            oi.setQuantity(qty);
            oi.setPrice(finalPrice);
            oi.setUnitOfOrdering(finalUom);

            double total = qty * finalPrice;
            oi.setTotal(total);
            oi.setExtendedQuantity(qty); // optional

            orderItems.add(oi);
        }

        order.setOrderItems(orderItems);
        return ordersRepository.save(order);
    }


    // -------------------------------------------------------------
    // 2) SEND ORDER (turn from DRAFT -> SENT, email the supplier)
    // -------------------------------------------------------------
    @Override
    public Orders sendOrder(Long orderId, String comments) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // For now, require the order be DRAFT before sending:
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Order must be DRAFT to send.");
        }

        // Validate we have a supplier
        Supplier supplier = order.getSentToSupplier();
        if (supplier == null) {
            throw new RuntimeException("Order is missing a supplier.");
        }

        // Find the default or fallback email from Supplier’s “orderEmails” set
        SupplierEmail selectedEmail = supplier.getOrderEmails().stream()
                .filter(SupplierEmail::isDefault)
                .findFirst()
                // .or(...) means “if the first is not found, pick any in the stream”
                .or(() -> supplier.getOrderEmails().stream().findFirst())
                .orElseThrow(() -> new RuntimeException(
                        "No email address found for supplier '" + supplier.getName()
                                + "'. Please define at least one order email."
                ));

        // Attempt sending email first
        String subject = "Purchase Order #" + order.getOrderNumber() + " Sent";
        String body = "Hello " + supplier.getName()
                + ",\n\nPlease see the attached order.\n\nThank you.";
        emailSenderService.sendEmail(selectedEmail.getEmail(), subject, body);

        // If email didn't throw, we assume success:
        order.setStatus(OrderStatus.SENT);
        order.setComments(comments);
        order.setSentDate(LocalDate.now());

        return ordersRepository.save(order);
    }

    // -------------------------------------------------------------
    // 3) RECEIVE ORDER (partial or full receiving with overrides)
    // -------------------------------------------------------------
    @Override
    public Orders receiveOrder(Long orderId, List<ReceiveLineDTO> lines, boolean updateOptionPrice) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.SENT && order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Order must be SENT or DRAFT to be received.");
        }

        boolean allFullyReceived = true;

        for (ReceiveLineDTO lineDto : lines) {
            // find the line, figure out finalQty, finalPrice, etc.
            OrderItem existingLine = findLineInOrder(order, lineDto.getOrderItemId());

            Double originalQty   = existingLine.getQuantity();
            Double originalPrice = existingLine.getPrice();

            Double finalQty   = (lineDto.getReceivedQty() != null)
                    ? lineDto.getReceivedQty() : originalQty;
            Double finalPrice = (lineDto.getFinalPrice() != null)
                    ? lineDto.getFinalPrice() : originalPrice;

            if (finalQty < originalQty) {
                allFullyReceived = false;
            }

            // update the line
            existingLine.setQuantity(finalQty);
            existingLine.setPrice(finalPrice);
            existingLine.setTotal(finalQty * finalPrice);

            // record the stock transaction with overridePrice=finalPrice
            stockTransactionService.recordPurchase(
                    order.getBuyerLocation(),
                    existingLine.getInventoryItem(),
                    finalQty,
                    existingLine.getUnitOfOrdering(),
                    order.getId(),
                    LocalDate.now(),
                    finalPrice,           // overridePrice
                    updateOptionPrice     // if user said yes, update purchaseOption
            );
        }

        // update order status
        if (allFullyReceived) {
            order.setStatus(OrderStatus.COMPLETED);
        } else {
            order.setStatus(OrderStatus.DELIVERED);
        }
        order.setDeliveryDate(LocalDate.now());

        return ordersRepository.save(order);
    }


    // -------------------------------------------------------------
    // 3a) Naive Moving-Average Cost Updating
    // -------------------------------------------------------------
    private void updateItemMovingAverageCost(InventoryItem item,
                                             Location location,
                                             double qtyReceived,
                                             double newUnitCost) {

        // Current on-hand (in item’s base UOM) from the ledger
        double currentOnHand = stockTransactionService.calculateTheoreticalOnHand(
                location.getId(),
                item.getId(),
                LocalDate.now()
        );

        // Old cost
        double oldCost = (item.getCurrentPrice() != null) ? item.getCurrentPrice() : 0.0;

        // total old value
        double oldValue = currentOnHand * oldCost;
        // add the new value
        double newValue = oldValue + (qtyReceived * newUnitCost);

        // after receiving, new OnHand is (currentOnHand + qtyReceived),
        // but we've already posted that purchase transaction, so
        // "theoreticalOnHand" now includes that new quantity.
        double updatedOnHand = stockTransactionService.calculateTheoreticalOnHand(
                location.getId(),
                item.getId(),
                LocalDate.now()
        );

        if (Math.abs(updatedOnHand) < 0.000001) {
            // no quantity => skip or set cost to something
            return;
        }

        double average = newValue / updatedOnHand;
        item.setCurrentPrice(average);
        inventoryItemRepository.save(item);
    }

    // -------------------------------------------------------------
    // 4) NO-ORDER INVOICE (receive items w/o an existing PO)
    // -------------------------------------------------------------
    @Override
    public Orders receiveWithoutOrder(Long companyId, NoOrderInvoiceDTO dto) {
        // Validate location & user & supplier (optionally)
        Location buyerLocation = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found: " + dto.getLocationId()));

        Users user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + dto.getUserId()));

        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found: " + dto.getSupplierId()));
        }

        // Create an “instant” completed order:
        Orders order = new Orders();
        order.setCompany(buyerLocation.getCompany());
        order.setBuyerLocation(buyerLocation);
        order.setCreatedByUser(user);
        order.setSentToSupplier(supplier);
        order.setCreationDate(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);
        order.setComments(dto.getComments());
        order.setOrderNumber("NO-ORDER-" + System.currentTimeMillis());
        order.setDeliveryDate(LocalDate.now());

        // Build lines
        List<OrderItem> lines = new ArrayList<>();
        for (NoOrderLineDTO lineDto : dto.getLines()) {
            InventoryItem item = inventoryItemRepository.findById(lineDto.getItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "InventoryItem not found: " + lineDto.getItemId()));

            UnitOfMeasure uom = null;
            if (lineDto.getUomId() != null) {
                uom = uomRepository.findById(lineDto.getUomId())
                        .orElseThrow(() -> new RuntimeException("UOM not found: " + lineDto.getUomId()));
            } else {
                uom = item.getInventoryUom();
            }

            Double qty   = lineDto.getQuantity();
            Double price = lineDto.getPrice();

            OrderItem oi = new OrderItem();
            oi.setOrders(order);
            oi.setInventoryItem(item);
            oi.setUnitOfOrdering(uom);
            oi.setQuantity(qty);
            oi.setPrice(price);
            oi.setTotal(qty * price);
            oi.setExtendedQuantity(qty);
            lines.add(oi);
        }
        order.setOrderItems(lines);

        // Save
        Orders saved = ordersRepository.save(order);

        // Post purchase for each line, update item cost
        for (OrderItem line : saved.getOrderItems()) {
            stockTransactionService.recordPurchase(
                    buyerLocation,
                    line.getInventoryItem(),
                    line.getQuantity(),
                    line.getUnitOfOrdering(),
                    saved.getId(),
                    LocalDate.now()
            );
            updateItemMovingAverageCost(
                    line.getInventoryItem(),
                    buyerLocation,
                    line.getQuantity(),
                    line.getPrice()
            );
        }

        return saved;
    }

    // -------------------------------------------------------------
    // 5) "FILL TO PAR" (auto-creating DRAFT Orders)
    // -------------------------------------------------------------
    @Override
    public List<Orders> fillToPar(Long locationId, Long userId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 1) find all bridging rows for that location (with parLevel, onHand, etc.)
        // In real code, you do:
        //   List<InventoryItemLocation> bridgingList = inventoryItemLocationRepository.findByLocationId(locationId);
        List<InventoryItemLocation> bridgingList = new ArrayList<>(); // placeholder

        // 2) We’ll group lines by the actual Supplier from each item’s “main” PurchaseOption
        //    That way we create a separate DRAFT order for each distinct supplier.
        Map<Supplier, List<OrderItem>> supplierToLines = new HashMap<>();

        for (InventoryItemLocation bil : bridgingList) {
            double par = (bil.getParLevel() != null) ? bil.getParLevel() : 0.0;
            if (par <= 0) continue;

            // OnHand (or call stockTransactionService.calcTheoreticalOnHand).
            double onHand = (bil.getOnHand() != null) ? bil.getOnHand() : 0.0;

            double shortage = par - onHand;
            if (shortage <= 0) continue; // no ordering needed

            InventoryItem item = bil.getInventoryItem();

            // 2a) Find the item’s “main or fallback” PurchaseOption
            PurchaseOption chosenPO = pickMainOrFallbackPurchaseOption(item);
            if (chosenPO == null) {
                // If an item truly has no purchase options, skip or throw.
                // Or we can’t know which supplier to use.
                continue;
            }

            Supplier sup = chosenPO.getSupplier();
            if (sup == null) {
                // If the PO doesn’t have a supplier, skip or throw
                continue;
            }

            // 2b) Price from the PO, fallback 0
            Double price = (chosenPO.getPrice() != null) ? chosenPO.getPrice() : 0.0;

            // 2c) Ordering UOM from the PO, fallback to item’s base
            UnitOfMeasure uom = (chosenPO.getOrderingUom() != null)
                    ? chosenPO.getOrderingUom()
                    : item.getInventoryUom();

            // 3) Build an OrderItem
            OrderItem oi = new OrderItem();
            oi.setInventoryItem(item);
            oi.setQuantity(shortage);
            oi.setPrice(price);
            oi.setTotal(shortage * price);
            oi.setExtendedQuantity(shortage);
            oi.setUnitOfOrdering(uom);

            // 4) group by the supplier
            supplierToLines.computeIfAbsent(sup, key -> new ArrayList<>()).add(oi);
        }

        // 5) For each supplier => create a DRAFT order with the lines
        List<Orders> newDrafts = new ArrayList<>();
        for (Map.Entry<Supplier, List<OrderItem>> e : supplierToLines.entrySet()) {
            Supplier sup = e.getKey();
            List<OrderItem> items = e.getValue();
            if (items.isEmpty()) continue;

            Orders draft = new Orders();
            draft.setCompany(location.getCompany());
            draft.setBuyerLocation(location);
            draft.setSentToSupplier(sup);
            draft.setCreatedByUser(user);
            draft.setCreationDate(LocalDateTime.now());
            draft.setStatus(OrderStatus.DRAFT);
            draft.setOrderNumber("PO-" + System.currentTimeMillis());

            // link lines
            for (OrderItem oi : items) {
                oi.setOrders(draft);
            }
            draft.setOrderItems(items);

            ordersRepository.save(draft);
            newDrafts.add(draft);
        }

        return newDrafts;
    }


    private PurchaseOption pickMainOrFallbackPurchaseOption(InventoryItem item) {
        // If there are no purchase options, return null (or throw)
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) {
            return null;
        }
        // 1) Try to find the main
        Optional<PurchaseOption> mainOpt = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) {
            return mainOpt.get();
        }
        // 2) fallback to the first
        return item.getPurchaseOptions().iterator().next();
    }

    // -------------------------------------------------------------
    // Utility to fetch order if you want
    // -------------------------------------------------------------
    private Orders getOrderById(Long orderId) {
        return ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private OrderItem findLineInOrder(Orders order, Long orderItemId) {
        return order.getOrderItems().stream()
                .filter(oi -> oi.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OrderItem not found in this order"));
    }

    @Override
    public List<Orders> findByCompanyAndDateRange(Long companyId, LocalDate start, LocalDate end) {
        // you can do a custom query in your repository or something simpler
        return ordersRepository.findByCompanyIdAndDateRange(companyId, start, end);
    }

    @Override
    public Orders getOrderById(Long companyId, Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        // confirm same company
        if (!order.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Order does not belong to company " + companyId);
        }
        return order;
    }

}
