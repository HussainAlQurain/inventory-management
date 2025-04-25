package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class OrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_history_id_seq")
    @SequenceGenerator(name = "order_history_id_seq", sequenceName = "order_history_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Orders orders;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime timestamp;

    private String comments;

}
