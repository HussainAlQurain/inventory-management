package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.AutoRedistributeSetting;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.dto.AutoRedistributeSettingDTO;
import com.rayvision.inventory_management.repository.AutoRedistributeSettingRepository;
import com.rayvision.inventory_management.repository.CompanyRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies/{companyId}/auto-redistribute-setting")
public class AutoRedistributeSettingController {

    private final CompanyRepository companyRepo;
    private final AutoRedistributeSettingRepository settingRepo;

    public AutoRedistributeSettingController(CompanyRepository companyRepo,
                                             AutoRedistributeSettingRepository settingRepo) {
        this.companyRepo = companyRepo;
        this.settingRepo = settingRepo;
    }

    /* ----------------------------------------------------  GET current */
    @GetMapping
    public ResponseEntity<AutoRedistributeSettingDTO> get(@PathVariable Long companyId) {

        AutoRedistributeSetting setting = settingRepo
                .findByCompanyId(companyId)
                .orElseGet(() -> {           // return a disabled “default view”
                    AutoRedistributeSetting s = new AutoRedistributeSetting();
                    s.setEnabled(false);
                    s.setFrequencySeconds(300);
                    return s;
                });

        return ResponseEntity.ok(toDto(setting));
    }

    /* ----------------------------------------------------  PUT up‑sert */
    @PutMapping
    public ResponseEntity<AutoRedistributeSettingDTO> upsert(@PathVariable Long companyId,
                                                             @Valid @RequestBody AutoRedistributeSettingDTO dto) {

        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        AutoRedistributeSetting setting = settingRepo
                .findByCompanyId(companyId)
                .orElseGet(() -> {
                    AutoRedistributeSetting s = new AutoRedistributeSetting();
                    s.setCompany(company);
                    return s;
                });

        /* ---- apply incoming values (null ⇒ keep existing) */
        if (dto.getEnabled() != null)           setting.setEnabled(dto.getEnabled());
        if (dto.getFrequencySeconds() != null)  setting.setFrequencySeconds(dto.getFrequencySeconds());
        if (dto.getAutoTransferComment() != null)
            setting.setAutoTransferComment(dto.getAutoTransferComment());

        AutoRedistributeSetting saved = settingRepo.save(setting);
        return ResponseEntity.ok(toDto(saved));
    }

    /* ---------------------------------------------------- helper */
    private AutoRedistributeSettingDTO toDto(AutoRedistributeSetting s) {
        AutoRedistributeSettingDTO d = new AutoRedistributeSettingDTO();
        d.setEnabled(s.isEnabled());
        d.setFrequencySeconds(s.getFrequencySeconds());
        d.setAutoTransferComment(s.getAutoTransferComment());
        return d;
    }


}
