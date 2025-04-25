package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_id_seq")
    @SequenceGenerator(name = "notification_id_seq", sequenceName = "notification_id_seq", allocationSize = 50)
    private Long id;
    private Long companyId;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private Boolean isRead;
}
