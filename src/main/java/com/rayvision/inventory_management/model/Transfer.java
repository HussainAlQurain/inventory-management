package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.SEQUENCE;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transfer_id_seq")
    @SequenceGenerator(name = "transfer_id_seq", sequenceName = "transfer_id_seq", allocationSize = 50)
    private Long id;

    private LocalDate creationDate;     // date/time the transfer record was created
    private LocalDate sentDate;          // NEW - date when the transfer was sent
    private LocalDate deliveryDate;      // NEW - date when the transfer was delivered
    private LocalDate completionDate;   // date/time the transfer was actually done
    
    @Enumerated(EnumType.STRING)
    private TransferStatus status;      // Now using enum instead of String

    /* audit ---------------------------------------------------------- */
    @ManyToOne @JoinColumn(name = "created_by_user_id", nullable = false)
    private Users createdByUser;        // manual or system-user

    @ManyToOne
    @JoinColumn(name = "from_location_id")
    private Location fromLocation;

    @ManyToOne
    @JoinColumn(name = "to_location_id")
    private Location toLocation;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransferLine> lines = new ArrayList<>();

    @Column(length = 255)
    private String comments;

}
