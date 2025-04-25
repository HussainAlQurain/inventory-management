package com.rayvision.inventory_management.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "company_user",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "users_id"})
)
public class CompanyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_user_id_seq")
    @SequenceGenerator(name = "company_user_id_seq", sequenceName = "company_user_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    @JsonBackReference("company-companyuser")
    Company company;

    @ManyToOne
    @JoinColumn(name = "users_id", nullable = false)
    @JsonBackReference("user-companyuser")
    Users user;

}
