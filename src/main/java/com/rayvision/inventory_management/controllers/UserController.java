package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.enums.userRoles;
import com.rayvision.inventory_management.mappers.UserResponseMapper;
import com.rayvision.inventory_management.mappers.impl.UserLoginMapperImpl;
import com.rayvision.inventory_management.mappers.impl.UserMapperImpl;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.LoginDTO;
import com.rayvision.inventory_management.model.dto.UserDTO;
import com.rayvision.inventory_management.model.dto.UserResponseDTO;
import com.rayvision.inventory_management.model.dto.UserUpdateDTO;
import com.rayvision.inventory_management.service.impl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    private UserMapperImpl userMapper;

    @Autowired
    private UserLoginMapperImpl userLoginMapper;
    
    @Autowired
    private UserResponseMapper userResponseMapper;

    /**
     * Create a new user within a company with a specified role
     */
    @PostMapping("/companies/{companyId}")
    public ResponseEntity<UserResponseDTO> createUserInCompany(
            @PathVariable Long companyId,
            @RequestBody UserDTO userDto) {
        
        // Map UserDto to Users entity
        Users user = userMapper.mapFrom(userDto);
        
        // Choose the role from DTO or default to ROLE_USER
        userRoles role = userDto.getRole() != null ? userDto.getRole() : userRoles.ROLE_USER;
        
        // Create user and associate with company
        Users savedUser = userService.createUser(user, companyId, role);
        
        // Convert to response DTO
        UserResponseDTO responseDto = userResponseMapper.toDto(savedUser);
        
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
    
    /**
     * Get all users for a specific company
     */
    @GetMapping("/companies/{companyId}")
    public ResponseEntity<List<UserResponseDTO>> getUsersByCompany(@PathVariable Long companyId) {
        List<Users> users = userService.findUsersByCompanyId(companyId);
        List<UserResponseDTO> responseList = userResponseMapper.toDtoList(users);
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Get a user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId) {
        return userService.findOne(userId)
                .map(userResponseMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Update user details
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateDTO updateDto) {
        
        Users updatedUser = userService.updateUser(userId, updateDto);
        UserResponseDTO responseDto = userResponseMapper.toDto(updatedUser);
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * Enable a user
     */
    @PatchMapping("/{userId}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long userId) {
        userService.toggleUserStatus(userId, true);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Disable a user (soft delete)
     */
    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long userId) {
        userService.toggleUserStatus(userId, false);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Change user role
     */
    @PatchMapping("/{userId}/roles")
    public ResponseEntity<UserResponseDTO> changeRole(
            @PathVariable Long userId,
            @RequestParam userRoles role) {
        
        UserUpdateDTO updateDto = new UserUpdateDTO();
        updateDto.setRole(role);
        
        Users updatedUser = userService.updateUser(userId, updateDto);
        UserResponseDTO responseDto = userResponseMapper.toDto(updatedUser);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("login")
    public ResponseEntity<String> login(@RequestBody LoginDTO loginDTO) {
        try {
            Users user = userLoginMapper.mapFrom(loginDTO);
            String authenticatedUser = userService.verify(user);
            return ResponseEntity.ok(authenticatedUser);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Invalid Username or Password\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Authentication failed\"}");
        }
    }

    /**
     * Get paginated users for a company with optional search term
     */
    @GetMapping("/companies/{companyId}/paginated")
    public ResponseEntity<Page<UserResponseDTO>> getPaginatedUsersByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort) {
        
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        
        Page<Users> usersPage = userService.findUsersByCompanyIdPaginated(companyId, search, pageable);
        Page<UserResponseDTO> responsePage = usersPage.map(userResponseMapper::toDto);
        
        return ResponseEntity.ok(responsePage);
    }
}
