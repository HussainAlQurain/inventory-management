package com.rayvision.inventory_management.model;


import com.rayvision.inventory_management.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class Orders {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private String orderNumber;
    private LocalDateTime creationDate;
    private LocalDate sentDate;
    private LocalDate deliveryDate;

    @ManyToOne
    @JoinColumn(name = "buyer_location_id", nullable = false)
    private Location buyerLocation; // The location that placed the order

    @ManyToOne
    @JoinColumn(name = "sent_by_location_id", nullable = false)
    private Location sentByLocation; // The location sending the order

    @ManyToOne
    @JoinColumn(name = "sent_to_supplier_id", nullable = false)
    private Supplier sentToSupplier; // The supplier receiving the order

    private Double estimatedPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private Users createdByUser; // The user who created the order

    private String comments;

    private Double totalExcludingTax;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderHistory> orderHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_user_id")
    private Users sentByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
