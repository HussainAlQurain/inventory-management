package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Notification;
import com.rayvision.inventory_management.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public void createNotification(Long companyId, String title, String message) {
        Notification n = new Notification();
        n.setCompanyId(companyId);
        n.setTitle(title);
        n.setMessage(message);
        n.setCreatedAt(LocalDateTime.now());
        n.setIsRead(false);
        notificationRepository.save(n);
    }
    
    public List<Notification> getAllNotificationsForCompany(Long companyId) {
        return notificationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }
    
    public List<Notification> getAllUnreadNotificationsForCompany(Long companyId) {
        return notificationRepository.findByCompanyIdAndIsReadOrderByCreatedAtDesc(companyId, false);
    }
    
    public List<Notification> getRecentUnreadNotificationsForCompany(Long companyId, int limit) {
        if (limit == 5) {
            // Use the optimized repository method for the common case
            return notificationRepository.findTop5ByCompanyIdAndIsReadOrderByCreatedAtDesc(companyId, false);
        }
        // Otherwise get all unread and limit in memory
        return getAllUnreadNotificationsForCompany(companyId).stream()
                .limit(limit)
                .toList();
    }
    
    public Optional<Notification> markAsRead(Long notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent()) {
            Notification n = notification.get();
            n.setIsRead(true);
            return Optional.of(notificationRepository.save(n));
        }
        return Optional.empty();
    }
    
    @Transactional
    public int markMultipleAsRead(List<Long> notificationIds) {
        return notificationRepository.markMultipleAsRead(notificationIds);
    }
}
