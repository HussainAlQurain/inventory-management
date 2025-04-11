package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Notification;
import com.rayvision.inventory_management.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

}
