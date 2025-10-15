package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.model.Notification;
import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.NotificationRepository;
import com.expensemate.expensemate_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ----------------- User Notifications -----------------

    // Get all notifications for a user (newest first)
    public List<Notification> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Mark a single notification as read
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        return notification;
    }

    // Mark all notifications as read for a user
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> notifications = notificationRepository.findByUser(user);
        notifications.forEach(n -> n.setRead(true));
    }

    // ----------------- Admin Notifications -----------------

    // Create notification for a single user
    public Notification createNotification(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    // Send global notification to all users (admin only)
    public void sendGlobalNotification(String message) {
        System.out.println("🔔 NotificationService: Starting global notification process");
        String adminMessage = message + " by admin";
        System.out.println("🔔 NotificationService: Admin message: " + adminMessage);
        
        List<User> allUsers = userRepository.findAll();
        System.out.println("🔔 NotificationService: Found " + allUsers.size() + " users to notify");
        
        allUsers.forEach(user -> {
            System.out.println("🔔 NotificationService: Creating notification for user: " + user.getEmail());
            createNotification(user.getId(), adminMessage);
        });
        
        System.out.println("✅ NotificationService: Global notification process completed");
    }

    // ----------------- Helper Methods for Common Scenarios -----------------

    public void notifyExpenseAdded(Long userId, double amount, String category) {
        String msg = String.format("Expense of ₹%.2f added to category %s.", amount, category);
        createNotification(userId, msg);
    }

    public void notifyBudgetExceeded(Long userId, String category) {
        String msg = String.format("Warning: You have exceeded your budget for %s.", category);
        createNotification(userId, msg);
    }

    public void notifyBudgetNearingLimit(Long userId, String category) {
        notifyBudgetNearingLimit(userId, category, 80); // default 80%
    }

    public void notifyBudgetNearingLimit(Long userId, String category, double percent) {
        String msg = String.format("Alert: You have spent %.0f%% of your budget for %s.", percent, category);
        createNotification(userId, msg);
    }

    // ✅ Prevent duplicate "Your budget report is ready" notifications
    public void notifyReportReady(Long userId) {
        String message = "Your budget report is ready.";

        // Fetch user and get recent notifications
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Fetch last few notifications for the user
        List<Notification> recent = notificationRepository.findByUserOrderByCreatedAtDesc(user);

        // Check if the same message already exists in the last 5 notifications
        boolean alreadySent = recent.stream()
                .limit(5)
                .anyMatch(n -> n.getMessage().equals(message));

        if (!alreadySent) {
            createNotification(userId, message);
        }
    }


    public void notifyAdminReminder(Long userId, String message) {
        createNotification(userId, "Reminder from admin: " + message);
    }
}
