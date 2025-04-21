package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RedistributeJob {
    private final AutoRedistributeSettingRepository settingRepo;
    private final LocationRepository locationRepo;
    private final InventoryItemLocationRepository iilRepo;
    private final AssortmentLocationRepository alRepo;
    private final InventoryItemRepository itemRepo;
    private final TransferService transferService;
    private final NotificationService notificationService;

    /* ---------------------------------------------------------------- */
    @Scheduled(fixedDelay = 60_000)               // run every 60 s
    public void run() {

        LocalDateTime now = LocalDateTime.now();

        for (AutoRedistributeSetting cfg : settingRepo.findByEnabledTrue()) {

            if (cfg.getFrequencySeconds()==null || cfg.getFrequencySeconds()<=0) continue;
            LocalDateTime nextRun = Optional.ofNullable(cfg.getLastCheckTime())
                    .orElse(LocalDateTime.of(1970,1,1,0,0))
                    .plusSeconds(cfg.getFrequencySeconds());
            if (now.isBefore(nextRun)) continue;      // not yet

            try {
                redistributeForCompany(cfg);
                cfg.setLastCheckTime(now);
                settingRepo.save(cfg);
            } catch (Exception ex){
                /* log + leave lastCheckTime unchanged so we retry next tick */
            }
        }
    }

    /* ===================== REAL WORK ================================== */
    private void redistributeForCompany(AutoRedistributeSetting cfg) {

        Company co = cfg.getCompany();
        List<Location> locs = locationRepo.findByCompanyId(co.getId());
        if (locs.size()<2) return;   // nothing to juggle

        /* 1 . build assortment‑aware item sets per location -------------- */
        Map<Long, Set<InventoryItem>> itemsByLoc = new HashMap<>();
        for (Location l : locs) {
            List<AssortmentLocation> als = alRepo.findByLocationId(l.getId());
            if (als.isEmpty()) {
                itemsByLoc.put(l.getId(),
                        new HashSet<>(itemRepo.findByCompanyId(co.getId())));
            } else {
                Set<InventoryItem> set = new HashSet<>();
                als.forEach(al -> set.addAll(al.getAssortment().getInventoryItems()));
                itemsByLoc.put(l.getId(), set);
            }
        }

        /* 2 . scan InventoryItemLocation rows --------------------------- */
        List<InventoryItemLocation> allRows =
                iilRepo.findAllByCompanyId(co.getId());   // custom query

        /* Build surplus / deficit buckets:
              surplusMap:   item‑id -> list< Surplus(fromLoc, qtyOverPar) >
              deficitMap:   item‑id -> list< Deficit(toLoc, abs(min-onHand)) >
        */
        Map<Long,List<Surplus>> surplusMap = new HashMap<>();
        Map<Long,List<Deficit>> deficitMap = new HashMap<>();

        for (InventoryItemLocation row : allRows) {

            Long locId  = row.getLocation().getId();
            Long itemId = row.getInventoryItem().getId();

            /* ignore if item NOT in that location’s assortment set */
            if (!itemsByLoc.getOrDefault(locId,Set.of()).contains(row.getInventoryItem())) continue;

            double onHand = Optional.ofNullable(row.getOnHand()).orElse(0.0);
            double min    = Optional.ofNullable(row.getMinOnHand()).orElse(0.0);
            double par    = Optional.ofNullable(row.getParLevel()).orElse(0.0);

            /* If min & par are 0 => skip (user opted‑out)                 */
            if (min<=0 || par<=0) continue;

            /* Surplus if onHand > par (use par>0)                         */
            if (par>0 && onHand>par) {
                double excess = onHand - par;
                surplusMap.computeIfAbsent(itemId,k->new ArrayList<>())
                        .add(new Surplus(locId, excess));
            }

            /* Deficit if onHand < min (use min>0)                         */
            if (min>0 && onHand<min) {
                double shortage = min - onHand;
                deficitMap.computeIfAbsent(itemId,k->new ArrayList<>())
                        .add(new Deficit(locId, shortage));
            }
        }

        /* 3 . simple greedy matching:                                    */
        for (Long itemId : deficitMap.keySet()) {

            List<Deficit> deficits = deficitMap.get(itemId);
            List<Surplus> surpluses = new ArrayList<>(
                    surplusMap.getOrDefault(itemId, Collections.emptyList()));

            /* sort deficits (most urgent first) & surpluses (most excess) */
            deficits.sort(Comparator.comparingDouble(Deficit::qty).reversed());
            surpluses.sort(Comparator.comparingDouble(Surplus::getQty).reversed());

            for (Deficit def : deficits) {
                double need = def.qty();
                Iterator<Surplus> it = surpluses.iterator();

                while (need>0 && it.hasNext()) {
                    Surplus sup = it.next();

                    double sendQty = Math.min(sup.getQty(), need);

                    /* build or update DRAFT transfer ------------------- */
                    Location fromLoc = findLoc(locs, sup.getLocId());
                    Location toLoc   = findLoc(locs, def.locId());

                    Transfer draft = transferService
                            .findDraftBetween(fromLoc.getId(), toLoc.getId());

                    List<ShortLine> single =
                            List.of(new ShortLine(itemId, sendQty,
                                    rowUom(itemId),         // helper
                                    fromLoc.getName(),
                                    toLoc.getName()));

                    if (draft==null){
                        /* create new draft */
                        TransferCreateDTO dto = new TransferCreateDTO();
                        dto.setFromLocationId(fromLoc.getId());
                        dto.setToLocationId(toLoc.getId());

                        TransferLineDTO l = new TransferLineDTO();
                        l.setInventoryItemId(itemId);
                        l.setQuantity(sendQty);
                        l.setUnitOfMeasureId(rowUom(itemId).getId());
                        dto.setLines(List.of(l));

                        Transfer newDraft = transferService.createTransfer(dto);

                        notificationService.createNotification(
                                co.getId(),"Auto‑Transfer Created",
                                "Draft transfer "+newDraft.getId()
                                        +" from "+fromLoc.getName()
                                        +" to "+toLoc.getName()
                                        +" ("+sendQty+") created by system");
                    } else {
                        transferService.updateDraftWithLines(
                                draft, single,
                                cfg.getAutoTransferComment()!=null?
                                        cfg.getAutoTransferComment():
                                        "Auto‑redistribute");

                        notificationService.createNotification(
                                co.getId(),"Auto‑Transfer Updated",
                                "Draft transfer "+draft.getId()
                                        +" updated (+"+sendQty+")");
                    }

                    /* update needs / surplus                             */
                    sup.setQty(sup.getQty() - sendQty);
                    need -= sendQty;
                    if (sup.getQty()<=0.0001) it.remove();
                }
            }
        }
    }

    /* ---------- helpers -------------------------------------------- */
    private Location findLoc(List<Location> all, Long id){
        return all.stream().filter(l->l.getId().equals(id)).findFirst().orElseThrow();
    }
    private UnitOfMeasure rowUom(Long itemId){
        return itemRepo.findById(itemId).orElseThrow().getInventoryUom();
    }

    /* Simple holder records */
    class Surplus {
        private final Long   locId;
        private       double qty;

        Surplus(Long locId, double qty) {
            this.locId = locId;
            this.qty   = qty;
        }

        Long   getLocId() { return locId; }
        double getQty()   { return qty; }
        void   setQty(double q) { this.qty = q; }
    }

    record Deficit (Long locId, double qty){}
    /** merged into TransferService.updateDraftWithLines(...)          */
    public record ShortLine(Long itemId,double qty,UnitOfMeasure uom,
                            String fromName,String toName){}

}
