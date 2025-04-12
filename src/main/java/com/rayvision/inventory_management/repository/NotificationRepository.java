package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Find all notifications for a company
    List<Notification> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    
    // Find unread notifications for a company
    List<Notification> findByCompanyIdAndIsReadOrderByCreatedAtDesc(Long companyId, Boolean isRead);
    
    // Find top N unread notifications for a company
    List<Notification> findTop5ByCompanyIdAndIsReadOrderByCreatedAtDesc(Long companyId, Boolean isRead);
    
    // Update multiple notifications to read status
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids")
    int markMultipleAsRead(@Param("ids") List<Long> notificationIds);
}
