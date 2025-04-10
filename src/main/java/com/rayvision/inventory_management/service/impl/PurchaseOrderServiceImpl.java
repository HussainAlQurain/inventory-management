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
    private final AssortmentLocationRepository assortmentLocationRepository;
    private final InventoryItemLocationRepository inventoryItemLocationRepository;

    public PurchaseOrderServiceImpl(
            OrderRepository ordersRepository,
            LocationRepository locationRepository,
            SupplierRepository supplierRepository,
            UserRepository userRepository,
            InventoryItemRepository inventoryItemRepository,
            UnitOfMeasureRepository uomRepository,
            StockTransactionService stockTransactionService,
            EmailSenderService emailSenderService,
            AssortmentLocationRepository assortmentLocationRepository,
            InventoryItemLocationRepository inventoryItemLocationRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.locationRepository = locationRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.uomRepository = uomRepository;
        this.stockTransactionService = stockTransactionService;
        this.emailSenderService = emailSenderService;
        this.assortmentLocationRepository = assortmentLocationRepository;
        this.inventoryItemLocationRepository = inventoryItemLocationRepository;
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
    public List<Orders> fillToPar(Long locationId, Long userId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Long companyId = location.getCompany().getId();

        // ---------------------------------------------------------
        // 1) Figure out which items are in scope for this location
        //    using the "assortmentLocation" bridging
        // ---------------------------------------------------------
        List<AssortmentLocation> bridgingAssortments = assortmentLocationRepository.findByLocationId(locationId);

        Set<InventoryItem> unionItems = new HashSet<>();
        if (bridgingAssortments.isEmpty()) {
            // fallback to ALL items in the company
            unionItems.addAll(inventoryItemRepository.findByCompanyId(companyId));
        } else {
            // gather items from each assortment
            for (AssortmentLocation al : bridgingAssortments) {
                unionItems.addAll(al.getAssortment().getInventoryItems());
            }
        }

        // ---------------------------------------------------------
        // 2) Retrieve or create InventoryItemLocation bridging rows
        //    to get parLevel & onHand for each item
        // ---------------------------------------------------------
        // We'll fetch all bridging rows in one pass from the DB
        List<InventoryItemLocation> bridgingList = inventoryItemLocationRepository.findByLocationId(locationId);

        // Convert bridgingList to a map: itemId -> bridging row
        Map<Long, InventoryItemLocation> iilMap = new HashMap<>();
        for (InventoryItemLocation bil : bridgingList) {
            iilMap.put(bil.getInventoryItem().getId(), bil);
        }

        // We'll store (supplier -> list of OrderItem) in a map
        Map<Supplier, List<OrderItem>> supplierToOrderLines = new HashMap<>();

        // ---------------------------------------------------------
        // 3) For each item in unionItems, find bridging row (if any),
        //    read parLevel & onHand, compute shortage
        // ---------------------------------------------------------
        for (InventoryItem item : unionItems) {
            InventoryItemLocation bil = iilMap.get(item.getId());
            if (bil == null) {
                // If there's no bridging row, we can either:
                // A) skip it
                // B) create a default bridging with par=0 => skip
                // For now, we skip if no bridging row is found,
                // or define par=0, onHand=0
                continue;
            }

            double par     = (bil.getParLevel() != null) ? bil.getParLevel() : 0.0;
            double onHand  = (bil.getOnHand()   != null) ? bil.getOnHand()   : 0.0;
            double shortage = par - onHand;

            if (shortage <= 0) {
                // no ordering needed
                continue;
            }

            // We must pick a PurchaseOption from the item that is "main or fallback"
            PurchaseOption chosenPO = pickMainOrFallbackPurchaseOption(item);
            if (chosenPO == null) {
                // can't place an order without a known supplier
                continue;
            }

            Supplier sup = chosenPO.getSupplier();
            if (sup == null) {
                // still can't place an order
                continue;
            }

            // Price from the PO, fallback 0
            Double price = (chosenPO.getPrice() != null) ? chosenPO.getPrice() : 0.0;

            // Ordering UOM from the PO, fallback to item’s base
            UnitOfMeasure uom = (chosenPO.getOrderingUom() != null)
                    ? chosenPO.getOrderingUom()
                    : item.getInventoryUom();

            // Build an OrderItem
            OrderItem oi = new OrderItem();
            oi.setInventoryItem(item);
            oi.setQuantity(shortage);
            oi.setPrice(price);
            oi.setTotal(shortage * price);
            oi.setExtendedQuantity(shortage);
            oi.setUnitOfOrdering(uom);

            // group by the supplier
            supplierToOrderLines.computeIfAbsent(sup, key -> new ArrayList<>()).add(oi);
        }

        // ---------------------------------------------------------
        // 4) Build one DRAFT order per Supplier
        // ---------------------------------------------------------
        List<Orders> newDrafts = new ArrayList<>();
        for (Map.Entry<Supplier, List<OrderItem>> e : supplierToOrderLines.entrySet()) {
            Supplier sup = e.getKey();
            List<OrderItem> lines = e.getValue();
            if (lines.isEmpty()) continue;

            Orders draft = new Orders();
            draft.setCompany(location.getCompany());
            draft.setBuyerLocation(location);
            draft.setSentByLocation(location); // if needed
            draft.setSentToSupplier(sup);
            draft.setCreatedByUser(user);
            draft.setCreationDate(LocalDateTime.now());
            draft.setStatus(OrderStatus.DRAFT);
            draft.setOrderNumber("PO-" + System.currentTimeMillis());

            // link lines
            for (OrderItem oi : lines) {
                oi.setOrders(draft);
            }
            draft.setOrderItems(lines);

            ordersRepository.save(draft);
            newDrafts.add(draft);
        }

        return newDrafts;
    }

    /**
     * If you want the same "main or fallback" logic
     * used in your other code
     */
    private PurchaseOption pickMainOrFallbackPurchaseOption(InventoryItem item) {
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) {
            return null;
        }
        // find main
        Optional<PurchaseOption> mainOpt = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) {
            return mainOpt.get();
        }
        // else first
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
    public List<Orders> findByCompanyAndDateRange(Long companyId, LocalDateTime start, LocalDateTime end) {
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
