package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    private final TransferService                   transferService;
    private final NotificationService               notificationService;

    /* ------------------------------------------------------------------
       scheduler wrapper
     ------------------------------------------------------------------ */
    @Scheduled(fixedDelay = 60_000)   // every 60 s
    public void run() {

        LocalDateTime now = LocalDateTime.now();

        for (AutoRedistributeSetting cfg : settingRepo.findByEnabledTrue()) {

            if (cfg.getFrequencySeconds() == null || cfg.getFrequencySeconds() <= 0) {
                continue; // mis‑configured
            }

            LocalDateTime next = Optional.ofNullable(cfg.getLastCheckTime())
                    .orElse(LocalDateTime.of(1970, 1, 1, 0, 0))
                    .plusSeconds(cfg.getFrequencySeconds());

            if (now.isBefore(next)) continue;        // cooldown not reached

            try {
                redistributeForCompany(cfg);
                cfg.setLastCheckTime(now);
                settingRepo.save(cfg);
            } catch (Exception ex) {
                log.error("Auto‑redistribute failed for company {}", cfg.getCompany().getId(), ex);
                // lastCheckTime unchanged → will retry next tick
            }
        }
    }

    /* ------------------------------------------------------------------
       core logic
     ------------------------------------------------------------------ */
    @Transactional
    private void redistributeForCompany(AutoRedistributeSetting cfg) {

        Company company = cfg.getCompany();
        List<Location> locs = locationRepo.findByCompanyId(company.getId());

        if (locs.size() < 2) return; // nothing to juggle

        /* 1️⃣ build: location‑id → allowed item set (assortment‑aware) */
        Map<Long, Set<Long>> allowedMap = buildAllowedItemMap(company.getId(), locs);

        /* 2️⃣ classify every InventoryItemLocation row */
        Map<Long, List<Surplus>> surplus = new HashMap<>();
        Map<Long, List<Deficit>> deficit = new HashMap<>();

        List<InventoryItemLocation> iilRows = iilRepo.findByLocationCompanyId(company.getId());
        if (iilRows.isEmpty()) {
            log.warn("Auto‑redistribute: no InventoryItemLocation rows for company {}", company.getId());
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

        /* 3️⃣ greedy match & draft creation */
        for (Long itemId : deficit.keySet()) {
            matchItem(itemId,
                    deficit.get(itemId),
                    surplus.getOrDefault(itemId, Collections.emptyList()),
                    locs,
                    cfg);
        }
    }

    /* ------------------------------------------------------------------
       helpers
     ------------------------------------------------------------------ */

    /** build location‑id → Set&lt;itemId&gt; map, honouring assortments */
    private Map<Long, Set<Long>> buildAllowedItemMap(long companyId, List<Location> locs) {

        Map<Long, Set<Long>> result = new HashMap<>();
        Set<Long> allCompanyItems   = itemRepo.findIdsByCompany(companyId); // add new lightweight query

        for (Location loc : locs) {
            List<AssortmentLocation> als = alRepo.findByLocationId(loc.getId());

            if (als.isEmpty()) {
                // no assortments for this loc → all items allowed (warehouse / default rule)
                result.put(loc.getId(), allCompanyItems);
            } else {
                Set<Long> ids = new HashSet<>();
                als.forEach(al ->
                        al.getAssortment()
                                .getInventoryItems()
                                .forEach(ii -> ids.add(ii.getId())));
                result.put(loc.getId(), ids);
            }
        }
        return result;
    }

    private void matchItem(Long itemId,
                           List<Deficit> deficits,
                           List<Surplus> surpluses,
                           List<Location> locs,
                           AutoRedistributeSetting cfg) {

        if (surpluses.isEmpty()) return; // no donor

        /* biggest shortages first, biggest surpluses first */
        deficits.sort(Comparator.comparingDouble(Deficit::qty).reversed());
        surpluses.sort(Comparator.comparingDouble(Surplus::qty).reversed());

        for (Deficit def : deficits) {

            double need = def.qty();
            Iterator<Surplus> it = surpluses.iterator();

            while (need > 0.0001 && it.hasNext()) {
                Surplus sup = it.next();

                /* do not drop donor below its MIN */
                double maxSend = Math.max(0, sup.qty() - 0.0001); // tiny epsilon
                if (maxSend <= 0) { it.remove(); continue; }

                double send = Math.min(maxSend, need);

                createOrUpdateDraft(itemId, send,
                        sup.locId(), def.locId(),
                        locs, cfg);

                sup.reduce(send);
                need -= send;

                if (sup.qty() <= 0.0001) it.remove();
            }
        }
    }

    /* ------------------------------------------------------------------ */

    private void createOrUpdateDraft(Long itemId, double qty,
                                     Long fromId, Long toId,
                                     List<Location> locs,
                                     AutoRedistributeSetting cfg) {

        Location from = findLoc(locs, fromId);
        Location to   = findLoc(locs, toId);

        Transfer draft = transferService.findDraftBetween(fromId, toId);

        if (draft == null) {
            /* create new draft */
            TransferCreateDTO dto = new TransferCreateDTO();
            dto.setFromLocationId(fromId);
            dto.setToLocationId(toId);

            TransferLineDTO line = new TransferLineDTO();
            line.setInventoryItemId(itemId);
            line.setQuantity(qty);
            line.setUnitOfMeasureId(requiredUom(itemId).getId());
            dto.setLines(List.of(line));

            draft = transferService.createTransfer(dto);

            notificationService.createNotification(
                    cfg.getCompany().getId(),
                    "Auto‑Transfer Created",
                    "Draft #" + draft.getId() + " | "
                            + from.getName() + " → " + to.getName()
                            + " (" + qty + ")");

        } else {
            /* append / merge */
            transferService.updateDraftWithLines(
                    draft,
                    List.of(new ShortLine(itemId, qty,
                            requiredUom(itemId),
                            from.getName(), to.getName())),
                    Optional.ofNullable(cfg.getAutoTransferComment())
                            .orElse("Auto‑redistribute"));

            notificationService.createNotification(
                    cfg.getCompany().getId(),
                    "Auto‑Transfer Updated",
                    "Draft #" + draft.getId() + " + " + qty);
        }
    }

    /* simple utilities */
    private Location findLoc(List<Location> all, Long id) {
        return all.stream()
                .filter(l -> l.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Location not found " + id));
    }

    private UnitOfMeasure requiredUom(Long itemId) {
        UnitOfMeasure uom = itemRepo.findById(itemId)
                .orElseThrow()
                .getInventoryUom();
        if (uom == null) throw new RuntimeException("Item " + itemId + " has no default UoM");
        return uom;
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
