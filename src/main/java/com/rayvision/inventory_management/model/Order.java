package com.rayvision.inventory_management.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class Order {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private long id;
    private String orderNumber;
    private LocalDate sentDate;
    private LocalDate deliveryDate;

    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private Location buyer;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private Double estimatedPrice;

    @Enumerated(EnumType.STRING)
    private  OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "sent_by_user_id")
    private Users sentBy;

    @ManyToOne
    @JoinColumn(name = "sent_to_user_id")
    private Users sentTo;

    private String comments;
    private Double totalExcludingTax;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderHistory> orderHistory;

}
