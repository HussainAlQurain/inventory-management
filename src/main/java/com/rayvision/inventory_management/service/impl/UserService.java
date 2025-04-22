package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.userRoles;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.UserUpdateDTO;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.CompanyUserRepository;
import com.rayvision.inventory_management.repository.RoleRepository;
import com.rayvision.inventory_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Transactional
    public Users createUser(Users user, Long companyId, userRoles roleToAssign) {
        // Check if company exists
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + companyId));

        // Encrypt password
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

        // Set default status if not provided
        if (user.getStatus() == null) {
            user.setStatus("active");
        }

        // Ensure no roles are set during the creation process by manually setting an empty set
        user.setRoles(new HashSet<>());

        // Save the user
        Users savedUser = userRepository.save(user);

        // Assign the specified role to the user
        Role role = roleRepository.findByName(roleToAssign);
        if (role == null) {
            throw new RuntimeException("Role not found: " + roleToAssign);
        }
        savedUser.getRoles().add(role);
        userRepository.save(savedUser);

        // Create company user relationship
        CompanyUser companyUser = new CompanyUser();
        companyUser.setCompany(company);
        companyUser.setUser(savedUser);
        companyUserRepository.save(companyUser);

        return savedUser;
    }

    public Users updateUser(Long userId, UserUpdateDTO updateDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Update fields if provided in the DTO
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getFirstName() != null) {
            user.setFirstName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null) {
            user.setLastName(updateDTO.getLastName());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getStatus() != null) {
            user.setStatus(updateDTO.getStatus());
        }

        // Handle role update if provided
        if (updateDTO.getRole() != null) {
            Role newRole = roleRepository.findByName(updateDTO.getRole());
            if (newRole == null) {
                throw new RuntimeException("Role not found: " + updateDTO.getRole());
            }

            // Replace all roles with the new role
            // This assumes a user has only one role at a time
            user.getRoles().clear();
            user.getRoles().add(newRole);
        }

        return userRepository.save(user);
    }

    public void toggleUserStatus(Long userId, boolean enable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(enable ? "active" : "disabled");
        userRepository.save(user);
    }

    public List<Users> findUsersByCompanyId(Long companyId) {
        // Get company user relationships for this company
        List<CompanyUser> companyUsers = companyUserRepository.findByCompanyId(companyId);

        // Extract user IDs
        List<Long> userIds = companyUsers.stream()
                .map(cu -> cu.getUser().getId())
                .collect(Collectors.toList());

        // Fetch all users in a single query
        return userRepository.findAllById(userIds);
    }

    public void assignRole(Users assigningUser, Users targetUser, userRoles roleToAssign) {
        // Find the role to assign in the database
        Role role = roleRepository.findByName(roleToAssign);
        if (role == null) {
            throw new RuntimeException("Role not found.");
        }

        // If this is the initial role assignment, allow assigning ROLE_USER directly
        if (assigningUser.getId() == targetUser.getId() && roleToAssign == userRoles.ROLE_USER) {
            // Assign the role directly to the user if they are creating their own default role assignment
            targetUser.getRoles().add(role);
            userRepository.save(targetUser);
            return;
        }

        // Check if assigning user has permission to assign the given role
        Set<Role> assigningUserRoles = assigningUser.getRoles();

        if (!assigningUserRoles.isEmpty()) {
            // Find the highest role level the assigning user has
            userRoles highestRole = getHighestRole(assigningUserRoles);

            // Ensure assigningUser has authority to assign the target role
            if (highestRole == null || roleToAssign.ordinal() > highestRole.ordinal()) {
                throw new RuntimeException("You do not have permission to assign this role.");
            }
        } else {
            // Assigning user has no roles and cannot assign roles (except initial case handled above)
            throw new RuntimeException("You do not have any roles and cannot assign roles.");
        }

        // Assign the role to the target user
        targetUser.getRoles().add(role);
        userRepository.save(targetUser);
    }

    private userRoles getHighestRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            // No roles assigned, return null
            return null;
        }

        // Find the highest role
        return roles.stream()
                .map(Role::getName)
                .max(Enum::compareTo)
                .orElse(null);
    }

    public String verify(Users user) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid Username or password");
        }

        if (authentication.isAuthenticated()) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Users fullUser = userPrincipal.getUser();
            List<Long> companyIds = companyUserRepository.findCompanyIdsByUserId(fullUser.getId());

            // Prepare claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", fullUser.getId());
            claims.put("roles", fullUser.getRoles());
            claims.put("email", fullUser.getEmail());
            claims.put("firstName", fullUser.getFirstName());
            claims.put("lastName", fullUser.getLastName());
            claims.put("companyIds", companyIds); // Add company IDs here

            // Pass claims to generateToken
            return jwtService.generateToken(fullUser, claims);
        }
        throw new RuntimeException("Authentication failed");
    }

    public Optional<Users> findOne(Long id) {
        Optional<Users> user = userRepository.findById(id);
        if (user.isPresent()) {
            user.get().setPassword("");
            return user;
        }
        return user;
    }
}
