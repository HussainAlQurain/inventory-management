package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.TransferStatus;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.TransferService;
import com.rayvision.inventory_management.util.SystemUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Redistributes surplus stock from one location to another location whose
 * on-hand is below its MIN (or PAR).
 *
 * • Surplus  = onHand − target  (target = PAR if set, otherwise MIN)
 * • Shortage = target − onHand  (only when onHand < MIN)
 *
 * Draft transfers are appended or created; nothing is sent if the donor
 * would drop below its own MIN.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedistributeJob {

    /* ──────────────────────────────────────────────────────────────
       Collaborators
       ────────────────────────────────────────────────────────────── */
    private final AutoRedistributeSettingRepository settingRepo;
    private final LocationRepository                locationRepo;
    private final InventoryItemLocationRepository   iilRepo;
    private final AssortmentLocationRepository      alRepo;
    private final InventoryItemRepository           itemRepo;
    private final UnitOfMeasureRepository           uomRepository;
    private final TransferService                   transferService;
    private final NotificationService               notificationService;
    private final SystemUserResolver                systemUserResolver;
    private final TransferRepository                transferRepo;
    private final UserRepository                    userRepository;

    /* ──────────────────────────────────────────────────────────────
       Scheduler entry-point
       ────────────────────────────────────────────────────────────── */
    @Async("redistributeExecutor")
    public CompletableFuture<Boolean> processRedistributeAsync(Long settingId) {
        try {
            AutoRedistributeSetting cfg =
                    settingRepo.findByIdWithCompany(settingId)
                            .orElseThrow(() -> new RuntimeException("Setting not found: " + settingId));

            if (!cfg.isEnabled()) {
                log.debug("Skipping redistribute for setting ID {} – disabled", settingId);
                return CompletableFuture.completedFuture(false);
            }

            redistributeForCompany(cfg);

            // update last-check timestamp (best-effort)
            settingRepo.findById(settingId).ifPresent(s -> {
                s.setLastCheckTime(LocalDateTime.now());
                settingRepo.save(s);
            });

            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.error("Auto-redistribute failed for setting {}", settingId, ex);
            return CompletableFuture.completedFuture(false);
        }
    }

    /* ──────────────────────────────────────────────────────────────
       Core algorithm
       ────────────────────────────────────────────────────────────── */
    @Transactional
    public void redistributeForCompany(AutoRedistributeSetting cfg) {

        if (cfg.getCompany() == null) {
            log.warn("Company is null for setting ID {}", cfg.getId());
            return;
        }

        Long companyId = cfg.getCompany().getId();
        List<Location> locs = locationRepo.findByCompanyId(companyId);

        if (locs.size() < 2) {
            log.debug("Company {} has <2 locations – skipping", companyId);
            return;
        }

        /* 1️⃣  Bulk-load all quantity facts (outgoing / incoming transfers) */
        Map<Long, Map<Long, Double>> outMap = to2LevelMap(transferRepo.getOutgoingQty());
        Map<Long, Map<Long, Double>> inMap  = to2LevelMap(transferRepo.getIncomingQty());

        Map<Long, Set<Long>> allowedMap = buildAllowedItemMap(companyId, locs);

        Map<Long, List<Surplus>> surplus = new HashMap<>();
        Map<Long, List<Deficit>> deficit = new HashMap<>();
        Map<Pair<Long,Long>, Boolean> stillNeeded = new HashMap<>();   // (fromLoc, toLoc)

        /* 2️⃣  Scan all InventoryItemLocation rows */
        for (InventoryItemLocation row : iilRepo.findByLocationCompanyId(companyId)) {

            long locId  = row.getLocation().getId();
            long itemId = row.getInventoryItem().getId();

            if (!allowedMap.getOrDefault(locId, Set.of()).contains(itemId)) continue;

            double onHand       = nz(row.getOnHand());
            double min          = nz(row.getMinOnHand());
            double par          = nz(row.getParLevel());
            if (min <= 0 && par <= 0) continue;           // nothing configured

            double committedOut = outMap.getOrDefault(itemId, Map.of()).getOrDefault(locId, 0d);
            double committedIn  =  inMap.getOrDefault(itemId, Map.of()).getOrDefault(locId, 0d);
            double futureStock  =  onHand - committedOut + committedIn;

            double target       =  (par > 0) ? par : min;
            double surplusQty   =  Math.max(0, futureStock - target);
            double shortageQty  =  (futureStock < min) ? (target - futureStock) : 0;

            if (surplusQty  > 0.0001)
                surplus .computeIfAbsent(itemId,k->new ArrayList<>())
                        .add(new Surplus(locId, surplusQty, min));

            if (shortageQty > 0.0001)
                deficit .computeIfAbsent(itemId,k->new ArrayList<>())
                        .add(new Deficit(locId, shortageQty));
        }

        /* 3️⃣  Greedy matching & draft creation */
        for (Long itemId : deficit.keySet()) {
            matchItemTransaction(itemId,
                    deficit .get(itemId),
                    surplus .getOrDefault(itemId, Collections.emptyList()),
                    locs, cfg, stillNeeded);
        }

        /* 4️⃣  Delete obsolete drafts (routes not in stillNeeded) */
        cleanupDraftTransfers(companyId, stillNeeded);

        log.info("Auto-redistribute finished for company {}", companyId);
    }

    /* ──────────────────────────────────────────────────────────────
       Draft cleanup
       ────────────────────────────────────────────────────────────── */
    private void cleanupDraftTransfers(long companyId,
                                       Map<Pair<Long,Long>,Boolean> stillNeeded) {

        Users sysUser = userRepository.findById(systemUserResolver.getSystemUserId())
                .orElseThrow(() -> new RuntimeException("System user not found"));

        /* loop over every system-draft for this company … */
        for (Transfer draft : transferRepo
                .findAllByCompanyAndStatusAndCreatedByUser(companyId, TransferStatus.DRAFT, sysUser)) {

            Pair<Long,Long> route = Pair.of(draft.getFromLocation().getId(),
                    draft.getToLocation().getId());

            /* If the current run did create/extend this route, keep it */
            if (stillNeeded.containsKey(route)) continue;

            Long toLocId  = draft.getToLocation().getId();
            Long fromLocId= draft.getFromLocation().getId();
            Long draftId  = draft.getId();

            /* Pre-load incoming/outgoing **excluding this draft** */
            Map<Long, Double> incMap = to1LevelMap(
                    transferRepo.getIncomingQtyExcludingDraft(toLocId, draftId));
            Map<Long, Double> outMap = to1LevelMap(
                    transferRepo.getOutgoingQtyExcludingDraft(fromLocId, draftId));

            boolean safeToDelete = true;

            /* For each line in the draft check that receiver stays ≥ target */
            for (TransferLine line : draft.getLines()) {

                Long itemId = line.getInventoryItem().getId();
                InventoryItemLocation row =
                        iilRepo.findByInventoryItemIdAndLocationId(itemId, toLocId)
                                .orElse(null);

                if (row == null) continue;       // item removed from assortment → safe

                double onHand = nz(row.getOnHand());
                double min    = nz(row.getMinOnHand());
                double par    = nz(row.getParLevel());
                double target = (par > 0) ? par : min;

                double futureAfterDeletion =
                        onHand                                     // physical stock
                                - outMap.getOrDefault(itemId, 0d)          // stuff still leaving
                                + incMap.getOrDefault(itemId, 0d);         // stuff still arriving
                /* we do NOT add the draft qty – we are simulating deletion! */

                if (futureAfterDeletion < target - 0.0001) {
                    safeToDelete = false;
                    break;
                }
            }

            if (safeToDelete) {
                transferService.deleteTransfer(draftId);
                notificationService.createNotification(
                        companyId, "Auto-Transfer Deleted",
                        "Deleted draft #" + draftId +
                                " – receiver(s) remain ≥ PAR/MIN");
            }
        }
    }

    /* helper – 2-column list → map<itemId,qty> */
    private Map<Long,Double> to1LevelMap(List<Object[]> rows){
        Map<Long,Double> m = new HashMap<>();
        for (Object[] r: rows)
            m.merge((Long)r[0], (Double)r[1], Double::sum);
        return m;
    }


    /* ──────────────────────────────────────────────────────────────
       Matching logic
       ────────────────────────────────────────────────────────────── */
    @Transactional
    public void matchItemTransaction(Long itemId,
                                     List<Deficit> deficits,
                                     List<Surplus> surpluses,
                                     List<Location> locs,
                                     AutoRedistributeSetting cfg,
                                     Map<Pair<Long,Long>,Boolean> stillNeeded) {

        if (cfg.getCompany() == null && cfg.getId() != null)
            cfg = settingRepo.findByIdWithCompany(cfg.getId()).orElse(cfg);

        matchItem(itemId, deficits, surpluses, locs, cfg, stillNeeded);
    }

    private void matchItem(Long itemId,
                           List<Deficit> deficits,
                           List<Surplus> surpluses,
                           List<Location> locs,
                           AutoRedistributeSetting cfg,
                           Map<Pair<Long,Long>,Boolean> stillNeeded) {

        if (surpluses.isEmpty()) return;

        deficits .sort(Comparator.comparingDouble(Deficit ::qty).reversed());
        surpluses.sort(Comparator.comparingDouble(Surplus::qty).reversed());

        for (Deficit def : deficits) {
            double need = def.qty();
            Iterator<Surplus> it = surpluses.iterator();

            while (need > 0.0001 && it.hasNext()) {
                Surplus sup = it.next();

                double maxSend = Math.max(0, sup.qty() - 0.0001);
                if (maxSend <= 0) { it.remove(); continue; }

                double send = Math.min(maxSend, need);

                createOrUpdateDraftTransaction(itemId, send,
                        sup.locId(), def.locId(),
                        locs, cfg);

                /* ✅ record that this donor→receiver route is still required */
                stillNeeded.put(Pair.of(sup.locId(), def.locId()), Boolean.TRUE);

                sup.reduce(send);
                need -= send;
                if (sup.qty() <= 0.0001) it.remove();
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────
       Draft creation / update
       ────────────────────────────────────────────────────────────── */
    @Transactional
    public void createOrUpdateDraftTransaction(Long itemId, double qty,
                                               Long fromId, Long toId,
                                               List<Location> locs,
                                               AutoRedistributeSetting cfg) {
        createOrUpdateDraft(itemId, qty, fromId, toId, locs, cfg);
    }

    private void createOrUpdateDraft(Long itemId, double qty,
                                     Long fromId, Long toId,
                                     List<Location> locs,
                                     AutoRedistributeSetting cfg) {

        Location from = findLocWithEagerFetch(locs, fromId);
        Location to   = findLocWithEagerFetch(locs, toId);
        if (from == null || to == null) return;

        Transfer draft = transferService.findDraftBetween(fromId, toId);

        if (draft == null) {                                     /* ── new draft ── */
            TransferCreateDTO dto = new TransferCreateDTO();
            dto.setFromLocationId(fromId);
            dto.setToLocationId  (toId);
            dto.setCreatedByUserId(systemUserResolver.getSystemUserId());

            UnitOfMeasure uom = requiredUomTransaction(itemId);
            if (uom == null) { log.error("No UoM for item {}", itemId); return; }

            TransferLineDTO line = new TransferLineDTO();
            line.setInventoryItemId(itemId);
            line.setQuantity(qty);
            line.setUnitOfMeasureId(uom.getId());
            dto.setLines(List.of(line));

            Transfer newDraft = transferService.createTransfer(dto);
            if (newDraft != null)
                notificationService.createNotification(
                        getCompanyIdSafely(from, to, cfg),
                        "Auto-Transfer Created",
                        "Draft #" + newDraft.getId() + " | " +
                                from.getName() + " → " + to.getName() +
                                " (" + qty + ")");
        }
        else {                                                    /* ── append line ── */
            UnitOfMeasure uom = requiredUomTransaction(itemId);
            if (uom == null) { log.error("No UoM for item {}", itemId); return; }

            transferService.updateDraftWithLines(
                    draft,
                    List.of(new ShortLine(itemId, qty, uom,
                            from.getName(), to.getName())),
                    Optional.ofNullable(cfg.getAutoTransferComment())
                            .orElse("Auto-redistribute"));
        }
    }

    /* ──────────────────────────────────────────────────────────────
       Misc helpers
       ────────────────────────────────────────────────────────────── */
    @Transactional public UnitOfMeasure requiredUomTransaction(Long itemId) {
        return requiredUom(itemId);
    }

    private UnitOfMeasure requiredUom(Long itemId) {
        InventoryItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found " + itemId));
        if (item.getInventoryUom() == null)
            throw new RuntimeException("Item " + itemId + " has no default UoM");

        return uomRepository.findById(item.getInventoryUom().getId())
                .orElseThrow(() -> new RuntimeException("UoM not found"));
    }

    private double nz(Double d) { return d != null ? d : 0.0; }

    private Map<Long, Map<Long, Double>> to2LevelMap(List<Object[]> rows) {
        Map<Long, Map<Long, Double>> m = new HashMap<>();
        for (Object[] r : rows) {
            Long itemId = (Long) r[0], locId = (Long) r[1]; Double q = (Double) r[2];
            m.computeIfAbsent(itemId,k->new HashMap<>()).merge(locId, q, Double::sum);
        }
        return m;
    }

    private Map<Long, Set<Long>> buildAllowedItemMap(long companyId, List<Location> locs) {
        Map<Long, Set<Long>> result = new HashMap<>();
        Set<Long> allItems = itemRepo.findIdsByCompany(companyId);
        for (Location loc : locs) {
            List<AssortmentLocation> als = alRepo.findByLocationId(loc.getId());
            if (als.isEmpty()) { result.put(loc.getId(), allItems); continue; }
            Set<Long> ids = new HashSet<>();
            als.forEach(al -> Optional.ofNullable(al.getAssortment())
                    .map(Assortment::getInventoryItems)
                    .ifPresent(list -> list.forEach(ii -> ids.add(ii.getId()))));
            result.put(loc.getId(), ids);
        }
        return result;
    }

    private Location findLocWithEagerFetch(List<Location> all, Long id) {
        return all.stream().filter(l -> l.getId().equals(id)).findFirst()
                .orElseGet(() -> locationRepo.findById(id).orElse(null));
    }

    private Long getCompanyIdSafely(Location from, Location to, AutoRedistributeSetting cfg) {
        if (cfg.getCompany() != null) return cfg.getCompany().getId();
        if (from.getCompany() != null) return from.getCompany().getId();
        return (to.getCompany() != null) ? to.getCompany().getId() : null;
    }

    /* value objects */
    private record Deficit(Long locId,double qty){}
    private static class Surplus{
        private final Long locId; private double qty; private final double donorMin;
        Surplus(Long l,double q,double m){locId=l;qty=q;donorMin=m;}
        Long locId(){return locId;} double qty(){return qty;} void reduce(double q){qty-=q;}
    }
    public record ShortLine(Long itemId,double qty,UnitOfMeasure uom,String fromName,String toName){}
}
