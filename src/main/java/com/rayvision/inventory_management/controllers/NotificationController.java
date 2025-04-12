package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.NotificationMapper;
import com.rayvision.inventory_management.model.Notification;
import com.rayvision.inventory_management.model.dto.BulkNotificationReadRequest;
import com.rayvision.inventory_management.model.dto.NotificationResponseDTO;
import com.rayvision.inventory_management.service.impl.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    
    @Autowired
    public NotificationController(NotificationService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }
    
    /**
     * Get the most recent 5 unread notifications for a company
     * 
     * @param companyId The ID of the company
     * @return A list of up to 5 most recent unread notifications
     */
    @GetMapping("/company/{companyId}/recent")
    public ResponseEntity<List<NotificationResponseDTO>> getRecentUnreadNotifications(@PathVariable Long companyId) {
        List<Notification> notifications = notificationService.getRecentUnreadNotificationsForCompany(companyId, 5);
        List<NotificationResponseDTO> dtos = notificationMapper.toDtoList(notifications);
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get all unread notifications for a company
     * 
     * @param companyId The ID of the company
     * @return A list of all unread notifications
     */
    @GetMapping("/company/{companyId}/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getAllUnreadNotifications(@PathVariable Long companyId) {
        List<Notification> notifications = notificationService.getAllUnreadNotificationsForCompany(companyId);
        List<NotificationResponseDTO> dtos = notificationMapper.toDtoList(notifications);
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get all notifications for a company
     * 
     * @param companyId The ID of the company
     * @return A list of all notifications
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotifications(@PathVariable Long companyId) {
        List<Notification> notifications = notificationService.getAllNotificationsForCompany(companyId);
        List<NotificationResponseDTO> dtos = notificationMapper.toDtoList(notifications);
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Mark a single notification as read
     * 
     * @param notificationId The ID of the notification to mark as read
     * @return The updated notification
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long notificationId) {
        Optional<Notification> result = notificationService.markAsRead(notificationId);
        return result.map(notification -> 
                ResponseEntity.ok(notificationMapper.toDto(notification)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Mark multiple notifications as read
     * 
     * @param request The request containing a list of notification IDs to mark as read
     * @return A response indicating how many notifications were marked as read
     */
    @PutMapping("/mark-read-batch")
    public ResponseEntity<Integer> markMultipleAsRead(@RequestBody BulkNotificationReadRequest request) {
        if (request.getNotificationIds() == null || request.getNotificationIds().isEmpty()) {
            return ResponseEntity.badRequest().body(0);
        }
        
        int updatedCount = notificationService.markMultipleAsRead(request.getNotificationIds());
        return ResponseEntity.ok(updatedCount);
    }
    
    /**
     * Create a test notification (for debugging/testing only)
     */
    @PostMapping("/company/{companyId}/test")
    public ResponseEntity<Void> createTestNotification(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "Test Notification") String title,
            @RequestParam(defaultValue = "This is a test notification message") String message) {
        
        notificationService.createNotification(companyId, title, message);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}