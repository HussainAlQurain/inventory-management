package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderItemDTO;
import com.rayvision.inventory_management.repository.AssortmentLocationRepository;
import com.rayvision.inventory_management.repository.AutoOrderSettingRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AutoOrderScheduledService {
    private final AutoOrderSettingRepository settingRepo;
    private final InventoryItemLocationService itemLocationService;
    private final PurchaseOrderService purchaseOrderService;
    private final NotificationService notificationService;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AssortmentLocationRepository assortmentLocationRepository;

    public AutoOrderScheduledService(
            AutoOrderSettingRepository settingRepo,
            InventoryItemLocationService itemLocationService,
            PurchaseOrderService purchaseOrderService,
            NotificationService notificationService,
            LocationRepository locationRepository,
            InventoryItemRepository inventoryItemRepository,
            AssortmentLocationRepository assortmentLocationRepository
    ) {
        this.settingRepo = settingRepo;
        this.itemLocationService = itemLocationService;
        this.purchaseOrderService = purchaseOrderService;
        this.notificationService = notificationService;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.assortmentLocationRepository = assortmentLocationRepository;
    }

    // runs every 60 seconds
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkAutoOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<AutoOrderSetting> allSettings = settingRepo.findAll();
        for (AutoOrderSetting setting : allSettings) {
            if (!setting.isEnabled()) {
                continue; // skip if not enabled
            }
            if (setting.getFrequencySeconds() == null || setting.getFrequencySeconds() <= 0) {
                continue; // skip invalid config
            }

            LocalDateTime last = Optional.ofNullable(setting.getLastCheckTime())
                    .orElse(LocalDateTime.of(1970,1,1,0,0));
            LocalDateTime nextAllowed = last.plusSeconds(setting.getFrequencySeconds());
            
            // Add debug logging for time check
            System.out.println("DEBUG: Setting for location " + 
                (setting.getLocation() != null ? setting.getLocation().getId() : "unknown") + 
                ", enabled=" + setting.isEnabled() + 
                ", lastCheck=" + setting.getLastCheckTime() + 
                ", nextAllowed=" + nextAllowed + 
                ", now=" + now + 
                ", should run=" + now.isAfter(nextAllowed));
                
            if (now.isAfter(nextAllowed)) {
                // time to run auto-order logic
                try {
                    runAutoOrderLogic(setting);
                    // if success, update lastCheckTime
                    setting.setLastCheckTime(now);
                    settingRepo.save(setting);
                } catch (Exception ex) {
                    // Add proper logging
                    ex.printStackTrace();
                    System.err.println("Error in auto-order for location ID " + 
                        (setting.getLocation() != null ? setting.getLocation().getId() : "unknown") + 
                        ": " + ex.getMessage());
                }
            }
        }
    }

    @Transactional
    public void runAutoOrderLogic(AutoOrderSetting setting) {
        Location loc = setting.getLocation();
        if (loc == null) {
            System.err.println("DEBUG: Location is null for setting ID: " + setting.getId());
            return;
        }
        
        System.out.println("DEBUG: Starting auto-order for location: " + loc.getId() + " - " + loc.getName());

        // 1) figure out which items are in scope
        //    same logic as fillToPar:
        Long companyId = loc.getCompany().getId();
        List<AssortmentLocation> bridgingAssortments =
                assortmentLocationRepository.findByLocationId(loc.getId());

        Set<InventoryItem> unionItems = new HashSet<>();
        if (bridgingAssortments.isEmpty()) {
            // fallback to ALL items in the company
            unionItems.addAll(inventoryItemRepository.findByCompanyId(companyId));
            System.out.println("DEBUG: No assortments found, using all company items: " + unionItems.size());
        } else {
            for (AssortmentLocation al : bridgingAssortments) {
                unionItems.addAll(al.getAssortment().getInventoryItems());
            }
            System.out.println("DEBUG: Using items from " + bridgingAssortments.size() + " assortments, total items: " + unionItems.size());
        }
        
        // After step 1:
        System.out.println("DEBUG: Found " + unionItems.size() + " items in scope");

        // 2) get itemLocation bridging rows
        List<InventoryItemLocation> bridgingList = itemLocationService.getByLocation(loc.getId());
        if (bridgingList.isEmpty()) {
            System.err.println("DEBUG: No inventory item location bridging rows found for location: " + loc.getId());
            return; // no items
        }
        
        System.out.println("DEBUG: Found " + bridgingList.size() + " bridging rows");
        
        // build a map itemId -> bridging row
        Map<Long, InventoryItemLocation> iilMap = new HashMap<>();
        for (InventoryItemLocation bil : bridgingList) {
            iilMap.put(bil.getInventoryItem().getId(), bil);
        }

        // 2.5) Get in-transit quantities from sent orders
        Map<Long, Double> inTransitMap = purchaseOrderService.calculateInTransitQuantitiesByLocation(loc.getId());
        System.out.println("DEBUG: Found " + inTransitMap.size() + " items with in-transit quantities");

        // 3) For each item in unionItems => compute shortage if par>0
        //    skip if both par & min are zero or null
        //    group by main/fallback purchase option's supplier
        Map<Supplier, List<ShortageLine>> shortageMap = new HashMap<>();
        int itemsProcessed = 0;
        int shortagesFound = 0;
        
        for (InventoryItem item : unionItems) {
            itemsProcessed++;
            InventoryItemLocation bil = iilMap.get(item.getId());
            if (bil == null) {
                // no bridging row => skip
                continue;
            }

            // if user wants "skip items with both minOnHand & par=0"
            double min = (bil.getMinOnHand() != null) ? bil.getMinOnHand() : 0.0;
            double par = (bil.getParLevel() != null) ? bil.getParLevel() : 0.0;
            if ( (min <= 0) && (par <= 0) ) {
                // skip if both are effectively zero => user doesn't want auto-order
                continue;
            }

            // compute shortage from par - onHand
            double onHand = (bil.getOnHand() != null) ? bil.getOnHand() : 0.0;
            
            // Account for in-transit quantities (already ordered but not received)
            double inTransitQty = inTransitMap.getOrDefault(item.getId(), 0.0);
            
            // Total expected quantity = current on hand + in transit
            double effectiveOnHand = onHand + inTransitQty;
            
            // Calculate shortage based on effective on-hand (including in-transit)
            double shortage = par - effectiveOnHand;
            
            if (shortage <= 0) {
                // no ordering needed
                continue;
            }
            
            shortagesFound++;
            System.out.println("DEBUG: Item " + item.getId() + " - " + item.getName() + 
                " has shortage of " + shortage + " (Par: " + par + ", OnHand: " + onHand + 
                ", In-transit: " + inTransitQty + ", Min: " + min + ")");

            // 4) pick an enabled purchase option => get supplier
            PurchaseOption po = pickEnabledMainOrFallbackPurchaseOption(item);
            if (po == null) {
                // no valid option => create a notification to user
                System.err.println("DEBUG: No enabled purchase option found for item: " + item.getId() + " - " + item.getName());
                notificationService.createNotification(
                        companyId,
                        "Auto-Order Skipped",
                        "Cannot auto-order item '" + item.getName()
                                + "' for location '" + loc.getName()
                                + "' because no enabled purchase option found."
                );
                continue;
            }

            Supplier sup = po.getSupplier();
            if (sup == null) {
                System.err.println("DEBUG: Purchase option has no supplier for item: " + item.getId() + " - " + item.getName());
                notificationService.createNotification(
                        companyId,
                        "Auto-Order Skipped",
                        "Cannot auto-order item '" + item.getName()
                                + "' for location '" + loc.getName()
                                + "' because purchase option has no supplier."
                );
                continue;
            }

            // record the shortage line
            ShortageLine sl = new ShortageLine(item.getId(), shortage, po.getPrice(), item.getName());
            shortageMap.computeIfAbsent(sup, k -> new ArrayList<>()).add(sl);
        }
        
        System.out.println("DEBUG: Processed " + itemsProcessed + " items, found " + shortagesFound + " with shortages");
        System.out.println("DEBUG: Found " + shortageMap.size() + " suppliers with shortages");

        // 5) create or update draft for each supplier
        for (Map.Entry<Supplier, List<ShortageLine>> e : shortageMap.entrySet()) {
            Supplier sup = e.getKey();
            List<ShortageLine> lines = e.getValue();
            if (lines.isEmpty()) continue;
            
            System.out.println("DEBUG: Processing supplier " + sup.getName() + " with " + lines.size() + " shortage lines");

            // find draft
            Orders draft = purchaseOrderService.findDraftOrderForSupplierAndLocation(sup.getId(), loc.getId());
            if (draft == null) {
                // create new
                OrderCreateDTO dto = new OrderCreateDTO();
                dto.setBuyerLocationId(loc.getId());
                dto.setSupplierId(sup.getId());
                dto.setCreatedByUserId(999999999L); // system user ID
                dto.setComments(
                        (setting.getAutoOrderComment() != null)
                                ? setting.getAutoOrderComment()
                                : "Auto-order by system"
                );

                List<OrderItemDTO> itemDtos = new ArrayList<>();
                for (ShortageLine sl : lines) {
                    OrderItemDTO idto = new OrderItemDTO();
                    idto.setInventoryItemId(sl.itemId());
                    idto.setQuantity(sl.shortageQty());
                    itemDtos.add(idto);
                }
                dto.setItems(itemDtos);
                
                System.out.println("DEBUG: Creating new draft order for supplier " + sup.getName() + " with " + itemDtos.size() + " items");

                try {
                    Orders createdDraft = purchaseOrderService.createOrder(loc.getCompany().getId(), dto);
                    System.out.println("DEBUG: Successfully created draft order with ID: " + createdDraft.getId());

                    notificationService.createNotification(
                            companyId,
                            "Auto-Order Created",
                            "Created new draft PO (ID=" + createdDraft.getId()
                                    + ") for supplier '" + sup.getName()
                                    + "', location '" + loc.getName() + "'"
                    );
                } catch (Exception ex) {
                    System.err.println("ERROR creating draft order: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                // update lines
                System.out.println("DEBUG: Updating existing draft order ID: " + draft.getId());
                try {
                    purchaseOrderService.updateDraftOrderWithShortages(draft, lines, setting.getAutoOrderComment());
                    System.out.println("DEBUG: Successfully updated draft order");

                    notificationService.createNotification(
                            companyId,
                            "Auto-Order Updated",
                            "Updated draft PO (ID=" + draft.getId()
                                    + ") for supplier '" + sup.getName()
                                    + "', location '" + loc.getName() + "'"
                    );
                } catch (Exception ex) {
                    System.err.println("ERROR updating draft order: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }


    private PurchaseOption pickMainOrFallbackPurchaseOption(InventoryItem item) {
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) return null;
        // find main
        Optional<PurchaseOption> mainOpt = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) return mainOpt.get();
        // else first
        return item.getPurchaseOptions().iterator().next();
    }

    private PurchaseOption pickEnabledMainOrFallbackPurchaseOption(InventoryItem item) {
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) {
            return null;
        }
        // Filter only enabled
        List<PurchaseOption> enabledOptions = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isOrderingEnabled)
                .toList();
        if (enabledOptions.isEmpty()) {
            return null;
        }

        // find main
        Optional<PurchaseOption> mainOpt = enabledOptions.stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) {
            return mainOpt.get();
        }
        // else the first enabled
        return enabledOptions.get(0);
    }


    // We'll define a small record for shortage lines
    public record ShortageLine(Long itemId, double shortageQty, Double price, String itemName) {}

}
