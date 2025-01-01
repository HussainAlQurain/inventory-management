package com.rayvision.inventory_management.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String email;
    private String status = "active";
    private String firstName;
    private String lastName;
    private String phone;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_companies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @JsonBackReference
    private Set<Company> companies = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonBackReference
    private Set<Role> roles = new HashSet<>();  // Initialize the set here to avoid errors

    @ManyToMany
    @JoinTable(
            name = "user_location",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    @JsonBackReference
    private Set<Location> locations;

    @JsonBackReference
    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> ordersCreated;

    @JsonBackReference
    @OneToMany(mappedBy = "sentByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> ordersSent;

}
