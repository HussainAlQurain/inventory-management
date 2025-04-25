package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Periodically redistributes surplus stock from one location
 * to another location whose on‑hand is below its MIN.
 *
 *  •  Surplus = onHand − target      (target = PAR if set, otherwise MIN)
 *  •  Shortage = target − onHand     (only when onHand &lt; MIN)
 *
 *  Draft transfers are appended or created; nothing is sent
 *  if the donor would drop below its own MIN.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedistributeJob {

    /* repositories & collaborators */
    private final AutoRedistributeSettingRepository settingRepo;
    private final LocationRepository                locationRepo;
    private final InventoryItemLocationRepository   iilRepo;
    private final AssortmentLocationRepository      alRepo;
    private final InventoryItemRepository           itemRepo;
    private final UnitOfMeasureRepository           uomRepository;
    private final TransferService                   transferService;
    private final NotificationService               notificationService;

    /* ------------------------------------------------------------------
       scheduler wrapper
     ------------------------------------------------------------------ */
    @Scheduled(fixedDelayString = "${inventory.scheduled.redistribute.delay:60000}")
    @Transactional // Removed readOnly = true
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        // Use the new repository method to eagerly fetch settings with companies
        List<AutoRedistributeSetting> enabledSettings = settingRepo.findByEnabledTrueWithCompany();

        log.debug("Checking {} auto-redistribute settings", enabledSettings.size());

        for (AutoRedistributeSetting cfg : enabledSettings) {
            if (cfg.getFrequencySeconds() == null || cfg.getFrequencySeconds() <= 0) {
                continue; // mis‑configured
            }

            LocalDateTime next = Optional.ofNullable(cfg.getLastCheckTime())
                    .orElse(LocalDateTime.of(1970, 1, 1, 0, 0))
                    .plusSeconds(cfg.getFrequencySeconds());

            if (now.isBefore(next)) continue;  // cooldown not reached

            try {
                // Extract IDs from eagerly loaded entities
                final Long companyId = cfg.getCompany() != null ? cfg.getCompany().getId() : null;
                final Long settingId = cfg.getId();

                if (companyId == null) {
                    log.error("Company not available for setting ID {}", settingId);
                    continue;
                }

                log.info("Starting auto-redistribute for company {}", companyId);

                CompletableFuture<Boolean> future = processRedistributeAsync(settingId);

                future.thenAccept(success -> {
                    if (success) {
                        try {
                            AutoRedistributeSetting freshCfg = settingRepo.findById(settingId)
                                    .orElseThrow(() -> new RuntimeException("Setting not found: " + settingId));
                            freshCfg.setLastCheckTime(LocalDateTime.now());
                            settingRepo.save(freshCfg);
                            log.debug("Updated lastCheckTime for setting {}", settingId);
                        } catch (Exception ex) {
                            log.error("Failed to update lastCheckTime for setting {}", settingId, ex);
                        }
                    }
                });
            } catch (Exception ex) {
                log.error("Failed to schedule auto‑redistribute for company {}",
                        (cfg.getCompany() != null ? cfg.getCompany().getId() : "unknown"), ex);
            }
        }
    }


    @Async("redistributeExecutor")
    public CompletableFuture<Boolean> processRedistributeAsync(Long settingId) {
        try {
            // Fetch setting with eagerly loaded company
            AutoRedistributeSetting cfg = settingRepo.findByIdWithCompany(settingId)
                    .orElseThrow(() -> new RuntimeException("Setting not found: " + settingId));
            redistributeForCompany(cfg);
            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.error("Auto‑redistribute failed for setting ID: {}", settingId, ex);
            return CompletableFuture.completedFuture(false);
        }
    }


    /* ------------------------------------------------------------------
       core logic
     ------------------------------------------------------------------ */
    @Transactional
    public void redistributeForCompany(AutoRedistributeSetting cfg) {
        LocalDateTime startTime = LocalDateTime.now();

        // Ensure we have a valid company ID
        if (cfg.getCompany() == null) {
            log.warn("Company is null for setting ID: {}", cfg.getId());
            return;
        }

        Long companyId = cfg.getCompany().getId();
        List<Location> locs = locationRepo.findByCompanyId(companyId);

        if (locs.size() < 2) {
            log.debug("Company {} has fewer than 2 locations - skipping redistribution", companyId);
            return; // nothing to juggle
        }

        Map<Long, Set<Long>> allowedMap = buildAllowedItemMap(companyId, locs);

        Map<Long, List<Surplus>> surplus = new HashMap<>();
        Map<Long, List<Deficit>> deficit = new HashMap<>();

        List<InventoryItemLocation> iilRows = iilRepo.findByLocationCompanyId(companyId);
        if (iilRows.isEmpty()) {
            log.warn("Auto‑redistribute: no InventoryItemLocation rows for company {}", companyId);
            return;
        }


        for (InventoryItemLocation row : iilRows) {
            long locId  = row.getLocation().getId();
            long itemId = row.getInventoryItem().getId();

            if (!allowedMap.getOrDefault(locId, Set.of()).contains(itemId)) continue;

            double onHand = nz(row.getOnHand());
            double min    = nz(row.getMinOnHand());
            double par    = nz(row.getParLevel());

            /* skip items that have neither MIN nor PAR configured */
            if (min <= 0 && par <= 0) continue;

            double target   = (par > 0) ? par : min;        // desired level
            double surplusQ = Math.max(0, onHand - target); // over target
            double shortage = (onHand < min) ? (target - onHand) : 0; // under min

            if (surplusQ > 0.0001) {
                surplus.computeIfAbsent(itemId, k -> new ArrayList<>())
                        .add(new Surplus(locId, surplusQ, min));
            }
            if (shortage > 0.0001) {
                deficit.computeIfAbsent(itemId, k -> new ArrayList<>())
                        .add(new Deficit(locId, shortage));
            }
        }

        log.debug("Company {} has {} items with surplus and {} items with deficit",
                companyId, surplus.size(), deficit.size());

        /* 3️⃣ greedy match & draft creation */
        int matchedItems = 0;
        for (Long itemId : deficit.keySet()) {
            matchItemTransaction(itemId,
                    deficit.get(itemId),
                    surplus.getOrDefault(itemId, Collections.emptyList()),
                    locs,
                    cfg);
            matchedItems++;
        }

        // Log the total execution time for performance monitoring
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("Auto-redistribute for company {} completed in {} ms (processed {} items)",
                companyId, duration.toMillis(), matchedItems);
    }

    /* ------------------------------------------------------------------
       Public transactional wrappers for private implementation methods
     ------------------------------------------------------------------ */

    // Public transactional wrapper for matchItem
    @Transactional
    public void matchItemTransaction(Long itemId,
                                     List<Deficit> deficits,
                                     List<Surplus> surpluses,
                                     List<Location> locs,
                                     AutoRedistributeSetting cfg) {
        // Ensure the company is eagerly fetched before entering the matchItem method
        if (cfg.getCompany() == null && cfg.getId() != null) {
            cfg = settingRepo.findByIdWithCompany(cfg.getId())
                  .orElse(cfg);  // Fall back to the original if not found
        }
        
        matchItem(itemId, deficits, surpluses, locs, cfg);
    }

    // Public transactional wrapper for createOrUpdateDraft
    @Transactional
    public void createOrUpdateDraftTransaction(Long itemId, double qty,
                                               Long fromId, Long toId,
                                               List<Location> locs,
                                               AutoRedistributeSetting cfg) {
        createOrUpdateDraft(itemId, qty, fromId, toId, locs, cfg);
    }

    // Public transactional wrapper for requiredUom
    @Transactional
    public UnitOfMeasure requiredUomTransaction(Long itemId) {
        return requiredUom(itemId);
    }

    /* ------------------------------------------------------------------
       helpers
     ------------------------------------------------------------------ */

    /** build location‑id → Set&lt;itemId&gt; map, honouring assortments */
    private Map<Long, Set<Long>> buildAllowedItemMap(long companyId, List<Location> locs) {
        Map<Long, Set<Long>> result = new HashMap<>();
        Set<Long> allCompanyItems   = itemRepo.findIdsByCompany(companyId); // add new lightweight query

        log.debug("Building allowed items map for {} locations with {} total items", locs.size(), allCompanyItems.size());

        for (Location loc : locs) {
            List<AssortmentLocation> als = alRepo.findByLocationId(loc.getId());

            if (als.isEmpty()) {
                // no assortments for this loc → all items allowed (warehouse / default rule)
                result.put(loc.getId(), allCompanyItems);
            } else {
                Set<Long> ids = new HashSet<>();
                als.forEach(al -> {
                    // Check for null before accessing
                    Assortment assortment = al.getAssortment();
                    if (assortment != null && assortment.getInventoryItems() != null) {
                        assortment.getInventoryItems().forEach(ii -> ids.add(ii.getId()));
                    }
                });
                result.put(loc.getId(), ids);
            }
        }
        return result;
    }

    // Changed from @Transactional private to non-transactional private
    private void matchItem(Long itemId,
                           List<Deficit> deficits,
                           List<Surplus> surpluses,
                           List<Location> locs,
                           AutoRedistributeSetting cfg) {
        if (surpluses.isEmpty()) return; // no donor

        /* biggest shortages first, biggest surpluses first */
        deficits.sort(Comparator.comparingDouble(Deficit::qty).reversed());
        surpluses.sort(Comparator.comparingDouble(Surplus::qty).reversed());

        log.debug("Matching item {} with {} locations in deficit and {} with surplus",
                itemId, deficits.size(), surpluses.size());

        for (Deficit def : deficits) {
            double need = def.qty();
            Iterator<Surplus> it = surpluses.iterator();

            while (need > 0.0001 && it.hasNext()) {
                Surplus sup = it.next();

                /* do not drop donor below its MIN */
                double maxSend = Math.max(0, sup.qty() - 0.0001); // tiny epsilon
                if (maxSend <= 0) { it.remove(); continue; }

                double send = Math.min(maxSend, need);

                log.debug("Location {} can send {} of item {} to location {}",
                        sup.locId(), send, itemId, def.locId());

                createOrUpdateDraftTransaction(itemId, send,
                        sup.locId(), def.locId(),
                        locs,
                        cfg);

                sup.reduce(send);
                need -= send;

                if (sup.qty() <= 0.0001) it.remove();
            }
        }
    }

    /* ------------------------------------------------------------------ */

    // Changed from @Transactional private to non-transactional private
    private void createOrUpdateDraft(Long itemId, double qty,
                                     Long fromId, Long toId,
                                     List<Location> locs,
                                     AutoRedistributeSetting cfg) {
        // Use a method that gets fully initialized locations
        Location from = findLocWithEagerFetch(locs, fromId);
        Location to = findLocWithEagerFetch(locs, toId);

        if (from == null || to == null) {
            log.error("Cannot create draft: from location {} or to location {} not found", fromId, toId);
            return;
        }

        log.debug("Processing transfer of {} for item {} from {} to {}",
                qty, itemId, from.getName(), to.getName());

        Transfer draft = transferService.findDraftBetween(fromId, toId);

        if (draft == null) {
            /* create new draft */
            TransferCreateDTO dto = new TransferCreateDTO();
            dto.setFromLocationId(fromId);
            dto.setToLocationId(toId);

            // Fetch UoM eagerly to prevent LazyInitialization - use transactional wrapper
            UnitOfMeasure uom = requiredUomTransaction(itemId);
            if (uom == null) {
                log.error("Cannot create draft: UoM not found for item {}", itemId);
                return;
            }

            TransferLineDTO line = new TransferLineDTO();
            line.setInventoryItemId(itemId);
            line.setQuantity(qty);
            line.setUnitOfMeasureId(uom.getId());
            dto.setLines(List.of(line));

            log.debug("Creating new transfer draft from {} to {}", from.getName(), to.getName());

            Transfer newDraft = transferService.createTransfer(dto);

            // Only create notification if transfer was successfully created
            if (newDraft != null && newDraft.getId() != null) {
                log.info("Created new auto-transfer draft #{} from {} to {}",
                        newDraft.getId(), from.getName(), to.getName());

                // Make sure we have a valid company ID for the notification
                Long companyId = getCompanyIdSafely(from, to, cfg);

                if (companyId != null) {
                    notificationService.createNotification(
                            companyId,
                            "Auto‑Transfer Created",
                            "Draft #" + newDraft.getId() + " | "
                                    + from.getName() + " → " + to.getName()
                                    + " (" + qty + ")");
                }
            }
        } else {
            /* Instead of relying on the service method, we need to ensure we use the repository's
               method that properly fetches the lines collection without causing LazyInitializationException */

            // Don't try to check for existing items here at all
            // Let transferService.updateDraftWithLines handle that for us inside its transaction

            String comment = Optional.ofNullable(cfg.getAutoTransferComment())
                    .orElse("Auto‑redistribute");

            log.debug("Updating existing transfer draft #{} from {} to {}",
                    draft.getId(), from.getName(), to.getName());

            UnitOfMeasure uom = requiredUomTransaction(itemId);
            if (uom == null) {
                log.error("Cannot update draft: UoM not found for item {}", itemId);
                return;
            }

            Transfer updatedDraft = transferService.updateDraftWithLines(
                    draft,
                    List.of(new ShortLine(itemId, qty,
                            uom,
                            from.getName(), to.getName())),
                    comment);

            log.debug("Updated transfer draft #{}", draft.getId());
        }
    }

    /* Get company ID safely from various sources */
    private Long getCompanyIdSafely(Location from, Location to, AutoRedistributeSetting cfg) {
        if (cfg.getCompany() != null) {
            return cfg.getCompany().getId();
        }
        if (from != null && from.getCompany() != null) {
            return from.getCompany().getId();
        }
        if (to != null && to.getCompany() != null) {
            return to.getCompany().getId();
        }
        log.warn("Could not determine company ID for notification");
        return null;
    }

    /* simple utilities */
    private Location findLocWithEagerFetch(List<Location> all, Long id) {
        // Try to find in the in-memory list first
        Optional<Location> found = all.stream()
                .filter(l -> l.getId().equals(id))
                .findFirst();

        if (found.isPresent()) {
            return found.get();
        }

        // If not found in the list, try to fetch from DB
        return locationRepo.findById(id).orElse(null);
    }

    private Location findLoc(List<Location> all, Long id) {
        return all.stream()
                .filter(l -> l.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Location not found " + id));
    }

    // Changed from @Transactional private to non-transactional private
    private UnitOfMeasure requiredUom(Long itemId) {
        try {
            // First, get the inventory item
            InventoryItem item = itemRepo.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

            if (item.getInventoryUom() == null) {
                throw new RuntimeException("Item " + itemId + " has no default UoM");
            }

            // Instead of using the potentially lazy-loaded UOM from the item,
            // directly fetch the UOM from the repository to ensure it's fully initialized
            Long uomId = item.getInventoryUom().getId();

            return uomRepository.findById(uomId)
                    .orElseThrow(() -> new RuntimeException("UnitOfMeasure not found: " + uomId));
        } catch (Exception e) {
            log.error("Error retrieving UOM for item {}: {}", itemId, e.getMessage());
            throw e;
        }
    }

    private double nz(Double d) { return d != null ? d : 0.0; }

    /* value objects */
    private record Deficit(Long locId, double qty) {}

    private static class Surplus {
        private final Long   locId;
        private final double donorMin; // cached for clarity
        private       double qty;
        Surplus(Long locId, double qty, double donorMin) {
            this.locId = locId;
            this.qty   = qty;
            this.donorMin = donorMin;
        }
        Long   locId() { return locId;        }
        double qty()   { return qty;          }
        void   reduce(double q){ this.qty -= q; }
    }

    /** record reused by TransferService.updateDraftWithLines(...) */
    public record ShortLine(Long itemId, double qty, UnitOfMeasure uom,
                            String fromName, String toName) {}
}