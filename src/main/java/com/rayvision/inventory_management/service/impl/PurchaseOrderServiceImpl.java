package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.exceptions.ResourceNotFoundException;
import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
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
    private final InventoryItemResponseMapper inventoryItemResponseMapper;

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
            InventoryItemLocationRepository inventoryItemLocationRepository,
            InventoryItemResponseMapper inventoryItemResponseMapper
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
        this.inventoryItemResponseMapper = inventoryItemResponseMapper;
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

    @Override
    public Orders findDraftOrderForSupplierAndLocation(Long supplierId, Long locationId) {
        try {
            return ordersRepository.findDraftBySupplierAndLocation(supplierId, locationId)
                .orElse(null);
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException ex) {
            // If multiple draft orders exist, log it and return the most recent one
            System.out.println("WARNING: Multiple draft orders found for supplier " + supplierId + 
                " and location " + locationId + ". Using the most recent one.");
            
            // Get all orders for this supplier/location with DRAFT status
            List<Orders> allDrafts = ordersRepository.findAllDraftsBySupplierAndLocation(supplierId, locationId);
            if (allDrafts.isEmpty()) {
                return null;
            }
            
            // Sort by creation date descending and return the most recent
            allDrafts.sort(Comparator.comparing(Orders::getCreationDate).reversed());
            return allDrafts.get(0);
        }
    }

    @Override
    @Transactional
    public Orders updateDraftOrderWithShortages(Orders draft, List<AutoOrderScheduledService.ShortageLine> lines, String comment) {
        boolean orderChanged = false;
        
        // 1) for each line => either add or update
        // We do a map: itemId -> existing orderItem
        Map<Long, OrderItem> existingMap = new HashMap<>();
        for (OrderItem oi : draft.getOrderItems()) {
            if (oi.getInventoryItem() != null) {
                existingMap.put(oi.getInventoryItem().getId(), oi);
            }
        }

        for (AutoOrderScheduledService.ShortageLine sl : lines) {
            if (existingMap.containsKey(sl.itemId())) {
                // update existing - REPLACE quantity instead of adding to it
                OrderItem oi = existingMap.get(sl.itemId());
                double currentQty = (oi.getQuantity() != null ? oi.getQuantity() : 0.0);
                double newQty = sl.shortageQty(); // Directly use the shortage quantity
                
                // Only update if quantity actually changed
                if (Math.abs(newQty - currentQty) > 0.001) {
                    oi.setQuantity(newQty);
                    oi.setTotal(newQty * oi.getPrice());
                    orderChanged = true;
                    System.out.println("DEBUG: Updated order line for item " + sl.itemId() + 
                        " from quantity " + currentQty + " to " + newQty);
                }
            } else {
                // create new line
                InventoryItem item = inventoryItemRepository.findById(sl.itemId())
                        .orElseThrow(() -> new RuntimeException("Item not found: " + sl.itemId()));
                // build an orderItem
                OrderItem newItem = new OrderItem();
                newItem.setOrders(draft);
                newItem.setInventoryItem(item);
                newItem.setQuantity(sl.shortageQty());
                // We'll rely on the "pick purchase option" logic or set price to item.getCurrentPrice() or PO price.
                newItem.setPrice(sl.price() != null ? sl.price() : 0.0);
                newItem.setUnitOfOrdering(item.getInventoryUom());
                double lineTotal = sl.shortageQty() * (newItem.getPrice() != null ? newItem.getPrice() : 0.0);
                newItem.setTotal(lineTotal);

                draft.getOrderItems().add(newItem);
                orderChanged = true;
                System.out.println("DEBUG: Created new order line for item " + sl.itemId() + 
                    " with quantity " + sl.shortageQty());
            }
        }

        // Only update the comment and save the order if something actually changed
        if (orderChanged) {
            // Replace the comment instead of appending to it
            draft.setComments(comment);
            return ordersRepository.save(draft);
        }
        
        // Return the draft without saving if nothing changed
        return draft;
    }

    /**
     * Calculates the quantity of each item that is currently in transit (ordered but not received)
     * for a specific location.
     * 
     * @param locationId The location ID to check for in-transit items
     * @return A map of inventory item IDs to their in-transit quantities
     */
    @Override
    public Map<Long, Double> calculateInTransitQuantitiesByLocation(Long locationId) {
        Map<Long, Double> inTransitMap = new HashMap<>();
        
        // Find all SENT orders for the location (orders that are sent but not delivered/completed)
        List<Orders> sentOrders = ordersRepository.findByBuyerLocationIdAndStatus(locationId, OrderStatus.SENT);
        
        // For each sent order, accumulate the quantities by item ID
        for (Orders order : sentOrders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getInventoryItem() != null && item.getQuantity() != null) {
                    Long itemId = item.getInventoryItem().getId();
                    Double quantity = item.getQuantity();
                    
                    // Add to existing quantity or set new quantity
                    inTransitMap.compute(itemId, (k, v) -> (v == null) ? quantity : v + quantity);
                }
            }
        }
        
        return inTransitMap;
    }

    // -------------------------------------------------------------
    // 6) UPDATE DRAFT ORDER (add/edit/delete lines, change comments)
    // -------------------------------------------------------------
    @Override
    @Transactional
    public Orders updateDraftOrder(Long orderId, OrderUpdateDTO dto) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        
        // Verify order is in DRAFT status
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Only orders in DRAFT status can be modified");
        }
        
        // Update order fields if provided in the DTO
        if (dto.getComments() != null) {
            order.setComments(dto.getComments());
        }
        
        if (dto.getSupplierId() != null && !dto.getSupplierId().equals(order.getSentToSupplier().getId())) {
            Supplier newSupplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", dto.getSupplierId()));
            order.setSentToSupplier(newSupplier);
            
            // When changing supplier, need to verify all order items have valid purchase options
            // with the new supplier. If not, remove invalid items.
            List<OrderItem> validItems = new ArrayList<>();
            for (OrderItem item : order.getOrderItems()) {
                boolean hasValidPurchaseOption = item.getInventoryItem().getPurchaseOptions()
                        .stream()
                        .anyMatch(po -> po.getSupplier() != null && 
                                po.getSupplier().getId().equals(newSupplier.getId()) && 
                                po.isOrderingEnabled());
                
                if (hasValidPurchaseOption) {
                    validItems.add(item);
                } else {
                    // Explicitly remove invalid items
                    order.getOrderItems().remove(item);
                }
            }
        }
        
        // Process item updates
        if (dto.getUpdatedItems() != null) {
            for (OrderItemUpdateDTO itemUpdate : dto.getUpdatedItems()) {
                OrderItem existingItem = findLineInOrder(order, itemUpdate.getOrderItemId());
                
                // Update quantity if provided
                if (itemUpdate.getQuantity() != null) {
                    existingItem.setQuantity(itemUpdate.getQuantity());
                    // Recalculate total
                    existingItem.setTotal(existingItem.getQuantity() * existingItem.getPrice());
                }
                
                // Update price if provided
                if (itemUpdate.getPrice() != null) {
                    existingItem.setPrice(itemUpdate.getPrice());
                    // Recalculate total
                    existingItem.setTotal(existingItem.getQuantity() * existingItem.getPrice());
                }
                
                // Update UOM if provided
                if (itemUpdate.getUomId() != null) {
                    UnitOfMeasure uom = uomRepository.findById(itemUpdate.getUomId())
                            .orElseThrow(() -> new ResourceNotFoundException("UnitOfMeasure", "id", itemUpdate.getUomId()));
                    existingItem.setUnitOfOrdering(uom);
                }
            }
        }
        
        // Process item deletions - proper JPA way to delete items from a collection with orphanRemoval
        if (dto.getDeletedItemIds() != null && !dto.getDeletedItemIds().isEmpty()) {
            // Iterate through a copy of the list to avoid ConcurrentModificationException
            List<OrderItem> itemsToRemove = new ArrayList<>();
            for (OrderItem item : order.getOrderItems()) {
                if (dto.getDeletedItemIds().contains(item.getId())) {
                    itemsToRemove.add(item);
                }
            }
            
            // Now remove each item from the original list
            for (OrderItem itemToRemove : itemsToRemove) {
                order.getOrderItems().remove(itemToRemove);
            }
        }
        
        // Process new items to add
        if (dto.getNewItems() != null && !dto.getNewItems().isEmpty()) {
            for (OrderItemDTO newItem : dto.getNewItems()) {
                InventoryItem invItem = inventoryItemRepository.findById(newItem.getInventoryItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", newItem.getInventoryItemId()));
                
                // Check that this item has a purchase option for the order's supplier
                Supplier supplier = order.getSentToSupplier();
                Set<PurchaseOption> matchingOptions = invItem.getPurchaseOptions().stream()
                        .filter(po -> po.getSupplier() != null && 
                                po.getSupplier().getId().equals(supplier.getId()) && 
                                po.isOrderingEnabled())
                        .collect(Collectors.toSet());
                
                if (matchingOptions.isEmpty()) {
                    throw new RuntimeException(
                        "No *enabled* purchase option found for item '" + invItem.getName()
                        + "' with supplier '" + supplier.getName() + "'. Cannot add to order.");
                }
                
                // Pick the main purchase option if available, otherwise the first one
                PurchaseOption chosenOption = matchingOptions.stream()
                        .filter(PurchaseOption::isMainPurchaseOption)
                        .findFirst()
                        .orElse(matchingOptions.iterator().next());
                
                Double finalPrice = (chosenOption.getPrice() != null) ? chosenOption.getPrice() : 0.0;
                Double qty = (newItem.getQuantity() != null) ? newItem.getQuantity() : 0.0;
                
                UnitOfMeasure finalUom = (chosenOption.getOrderingUom() != null)
                        ? chosenOption.getOrderingUom()
                        : invItem.getInventoryUom();
                
                OrderItem oi = new OrderItem();
                oi.setOrders(order);
                oi.setInventoryItem(invItem);
                oi.setQuantity(qty);
                oi.setPrice(finalPrice);
                oi.setUnitOfOrdering(finalUom);
                oi.setTotal(qty * finalPrice);
                oi.setExtendedQuantity(qty);
                
                order.getOrderItems().add(oi);
            }
        }
        
        return ordersRepository.save(order);
    }
    
    // -------------------------------------------------------------
    // 7) DELETE DRAFT ORDER (only if in DRAFT status)
    // -------------------------------------------------------------
    @Override
    @Transactional
    public void deleteDraftOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
                
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Only orders in DRAFT status can be deleted");
        }
        
        ordersRepository.delete(order);
    }
    
    // -------------------------------------------------------------
    // 8) GET AVAILABLE INVENTORY ITEMS BY SUPPLIER AND LOCATION
    // -------------------------------------------------------------
    @Override
    public List<InventoryItemResponseDTO> getInventoryItemsBySupplierAndLocation(Long supplierId, Long locationId) {
        // 1. First check if the location has any assortments with inventory items
        List<AssortmentLocation> assortmentLocations = assortmentLocationRepository.findByLocationId(locationId);
        Set<InventoryItem> assortmentItems = new HashSet<>();
        
        if (!assortmentLocations.isEmpty()) {
            // Collect all inventory items from all assortments linked to this location
            for (AssortmentLocation al : assortmentLocations) {
                if (al.getAssortment() != null && al.getAssortment().getInventoryItems() != null) {
                    assortmentItems.addAll(al.getAssortment().getInventoryItems());
                }
            }
        }
        
        // 2. Determine which list of items to filter against the supplier's purchase options
        List<InventoryItem> baseItemList;
        if (!assortmentItems.isEmpty()) {
            // Filter using assortment items as the base list
            baseItemList = new ArrayList<>(assortmentItems);
        } else {
            // If no assortment items, get all items from the company
            // We need to get the company ID from any object
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", supplierId));
            Long companyId = supplier.getCompany().getId();
            baseItemList = inventoryItemRepository.findByCompanyId(companyId);
        }
        
        // 3. Filter the baseItemList to only include items with valid purchase options for this supplier
        List<InventoryItem> filteredItems = baseItemList.stream()
            .filter(item -> {
                if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) {
                    return false;
                }
                
                return item.getPurchaseOptions().stream()
                    .anyMatch(po -> po.getSupplier() != null && 
                            po.getSupplier().getId().equals(supplierId) && 
                            po.isOrderingEnabled() &&
                            (po.isMainPurchaseOption() || true)); // Include if main or any enabled option exists
            })
            .collect(Collectors.toList());
        
        // 4. Convert entities to DTOs using the mapper
        return filteredItems.stream()
            .map(inventoryItemResponseMapper::toInventoryItemResponseDTO)
            .collect(Collectors.toList());
    }

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
    @Override
    public Page<Orders> searchOrders(Long companyId, Long supplierId, Long locationId, 
                                   OrderStatus status, LocalDateTime start, LocalDateTime end,
                                   Pageable pageable) {
        // Create a specification for dynamic filtering
        Specification<Orders> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Always filter by company ID
            predicates.add(criteriaBuilder.equal(root.get("company").get("id"), companyId));
            
            // Filter by date range
            if (start != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("creationDate"), start));
            }
            
            if (end != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("creationDate"), end));
            }
            
            // Filter by supplier if provided
            if (supplierId != null) {
                predicates.add(criteriaBuilder.equal(root.get("sentToSupplier").get("id"), supplierId));
            }
            
            // Filter by location if provided
            if (locationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("buyerLocation").get("id"), locationId));
            }
            
            // Filter by status if provided
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return ordersRepository.findAll(spec, pageable);
    }
}
