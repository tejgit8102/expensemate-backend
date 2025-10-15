package com.expensemate.expensemate_backend.repository;

import com.expensemate.expensemate_backend.model.Notification;
import com.expensemate.expensemate_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Fetch all notifications for a specific user, sorted by newest first
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Fetch only unread notifications for a user (sorted by newest first)
    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);

    // Fetch all notifications for a user (unsorted)
    List<Notification> findByUser(User user);

    // Mark all notifications as read for a specific user (optimized bulk update)
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllAsRead(User user);

    // Delete all notifications for a user (useful for cleanup)
    @Transactional
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user")
    void deleteByUser(User user);
}
