package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoRedistributeSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auto_redistribute_setting_id_seq")
    @SequenceGenerator(name = "auto_redistribute_setting_id_seq", sequenceName = "auto_redistribute_setting_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)               // ❶ company‑level
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private boolean enabled        = true;           // ❷ master switch
    private Integer frequencySeconds = 60;           // ❸ how often to run
    private LocalDateTime lastCheckTime;

    /** comment that will be added to every auto‑transfer draft           */
    private String autoTransferComment;

}
