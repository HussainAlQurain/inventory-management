package com.rayvision.inventory_management.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-companyuser")
    private Set<CompanyUser> companyUsers = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonBackReference
    private Set<Role> roles = new HashSet<>();  // Initialize the set here to avoid errors

    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-locationuser")
    private Set<LocationUser> locationUsers;

    @JsonIgnore
    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Orders> ordersCreated;

    @JsonIgnore
    @OneToMany(mappedBy = "sentByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Orders> ordersSent;

}
