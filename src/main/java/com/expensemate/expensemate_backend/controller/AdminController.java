
package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.dto.*;
import com.expensemate.expensemate_backend.model.*;
import com.expensemate.expensemate_backend.repository.ExpenseRepository;
import com.expensemate.expensemate_backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ExpenseRepository expenseRepository;

    public AdminController(AdminService adminService, ExpenseRepository expenseRepository) {
        this.adminService = adminService;
        this.expenseRepository = expenseRepository;
    }

    // --- Dashboard ---
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // --- Manage Users ---
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deactivated successfully.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/activate")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable Long id) {
        adminService.activateUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User activated successfully.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable Long id) {
        adminService.resetUserPassword(id);
        return ResponseEntity.ok("Password reset successfully.");
    }

    // --- Manage Expenses ---
    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(adminService.getAllExpenses());
    }

    @GetMapping("/expenses/flagged")
    public ResponseEntity<List<Expense>> getFlaggedExpenses() {
        return ResponseEntity.ok(adminService.getFlaggedExpenses());
    }

    @PutMapping("/expenses/{id}/flag")
    public ResponseEntity<Map<String, String>> flagExpense(@PathVariable Long id) {
        adminService.flagExpense(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Expense flagged as suspicious.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/expenses/{id}/unflag")
    public ResponseEntity<Map<String, String>> unflagExpense(@PathVariable Long id) {
        adminService.unflagExpense(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Expense unflagged successfully.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long id) {
        adminService.deleteExpense(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Expense deleted successfully.");
        return ResponseEntity.ok(response);
    }

    // --- Manage Budgets ---
    @GetMapping("/budgets/summary")
    public ResponseEntity<Map<String, Object>> getBudgetSummary() {
        return ResponseEntity.ok(adminService.getBudgetSummary());
    }

    // --- System Reports ---
    @GetMapping("/reports/system")
    public ResponseEntity<Map<String, Object>> getSystemReport() {
        System.out.println("🔍 AdminController: Getting system report...");
        Map<String, Object> report = adminService.getSystemReport();
        System.out.println("📊 AdminController: System report data: " + report);
        return ResponseEntity.ok(report);
    }

    // --- Debug endpoint ---
    @GetMapping("/debug/stats")
    public ResponseEntity<Map<String, Object>> getDebugStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", adminService.getAllUsers().size());
        stats.put("expenseCount", adminService.getAllExpenses().size());
        stats.put("flaggedExpenses", adminService.getFlaggedExpenses().size());
        stats.put("timestamp", java.time.LocalDateTime.now().toString());
        System.out.println("🐛 Debug stats: " + stats);
        return ResponseEntity.ok(stats);
    }

    // --- Debug expenses endpoint ---
    @GetMapping("/debug/expenses")
    public ResponseEntity<Map<String, Object>> getDebugExpenses() {
        Map<String, Object> debug = new HashMap<>();
        
        // Get all expenses
        List<Expense> allExpenses = adminService.getAllExpenses();
        debug.put("totalExpenses", allExpenses.size());
        
        // Get first few expenses for debugging
        List<Map<String, Object>> sampleExpenses = new ArrayList<>();
        for (int i = 0; i < Math.min(5, allExpenses.size()); i++) {
            Expense expense = allExpenses.get(i);
            Map<String, Object> expenseData = new HashMap<>();
            expenseData.put("id", expense.getId());
            expenseData.put("amount", expense.getAmount());
            expenseData.put("category", expense.getCategory());
            expenseData.put("date", expense.getDate());
            expenseData.put("description", expense.getDescription());
            sampleExpenses.add(expenseData);
        }
        debug.put("sampleExpenses", sampleExpenses);
        
        // Test category breakdown query
        try {
            List<Object[]> categoryData = expenseRepository.getCategoryBreakdown();
            debug.put("categoryQueryResult", categoryData.size());
            debug.put("categoryData", categoryData);
        } catch (Exception e) {
            debug.put("categoryQueryError", e.getMessage());
        }
        
        System.out.println("🐛 Debug expenses: " + debug);
        return ResponseEntity.ok(debug);
    }

    // --- Global Notifications ---
    @PostMapping("/notifications")
    public ResponseEntity<Map<String, String>> sendGlobalNotification(@RequestBody Map<String, String> body) {
        System.out.println("🔔 AdminController: Received global notification request");
        System.out.println("🔔 Request body: " + body);
        
        String message = body.get("message");
        if (message == null || message.isEmpty()) {
            System.out.println("❌ AdminController: Empty message received");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Message cannot be empty.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        System.out.println("🔔 AdminController: Sending notification with message: " + message);
        adminService.sendGlobalNotification(message);
        
        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("message", "Global notification sent successfully.");
        System.out.println("✅ AdminController: Notification sent successfully");
        return ResponseEntity.ok(successResponse);
    }
}
