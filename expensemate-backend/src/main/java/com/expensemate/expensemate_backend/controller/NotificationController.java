package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.model.Notification;
import com.expensemate.expensemate_backend.service.NotificationService;
import com.expensemate.expensemate_backend.service.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ----------------- User Endpoints -----------------

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getUserNotifications(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Notification> markNotificationRead(@PathVariable Long id) {
        Notification updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<String> markAllRead(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Unauthorized: No authentication found.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid principal type.");
        }

        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok("All notifications marked as read.");
    }
}
