package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.model.*;
import com.expensemate.expensemate_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        ExpenseRepository expenseRepository,
                        BudgetRepository budgetRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.notificationService = notificationService;
    }

    // --- Dashboard ---
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalExpenses", expenseRepository.count());
        stats.put("totalBudgets", budgetRepository.count());
        stats.put("recentExpenses", expenseRepository.findTop10ByOrderByDateDesc());
        return stats;
    }

    // --- User Management ---
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void resetUserPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword("{noop}default123"); // set default password or generate new
        userRepository.save(user);
    }

    // --- Expense Management ---
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public List<Expense> getFlaggedExpenses() {
        return expenseRepository.findByFlaggedTrue();
    }

    @Transactional
    public void flagExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setFlagged(true);
        expenseRepository.save(expense);
    }

    @Transactional
    public void unflagExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setFlagged(false);
        expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expenseRepository.delete(expense);
    }

    // --- Budget Summary ---
    public Map<String, Object> getBudgetSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBudgets", budgetRepository.count());
        summary.put("avgBudget", budgetRepository.getAverageBudgetAmount());
        summary.put("overLimitBudgets", budgetRepository.countBudgetsOverLimit());
        return summary;
    }

    // --- System Report ---
    public Map<String, Object> getSystemReport() {
        System.out.println("🔍 AdminService: Generating system report...");
        Map<String, Object> report = new HashMap<>();
        
        // Basic counts
        long userCount = userRepository.count();
        long expenseCount = expenseRepository.count();
        long budgetCount = budgetRepository.count();
        long flaggedCount = expenseRepository.findByFlaggedTrue().size();
        
        System.out.println("📊 Basic counts - Users: " + userCount + ", Expenses: " + expenseCount + ", Budgets: " + budgetCount + ", Flagged: " + flaggedCount);
        
        report.put("userCount", userCount);
        report.put("expenseCount", expenseCount);
        report.put("budgetCount", budgetCount);
        report.put("flaggedExpenses", flaggedCount);
        
        // Active users (users with expenses in last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long activeUsers = userRepository.countActiveUsers(thirtyDaysAgo);
        report.put("activeUsers", activeUsers);
        System.out.println("👥 Active users: " + activeUsers);
        
        // Latest users
        report.put("latestUsers", userRepository.findTop5ByOrderByCreatedAtDesc());
        
        // Recent expenses
        report.put("recentExpenses", expenseRepository.findTop10ByOrderByDateDesc());
        
        // Category breakdown
        System.out.println("📊 Getting category breakdown...");
        List<Object[]> categoryData = expenseRepository.getCategoryBreakdown();
        Map<String, Double> categoryBreakdown = new HashMap<>();
        System.out.println("📊 Category data size: " + categoryData.size());
        
        for (Object[] row : categoryData) {
            String category = (String) row[0];
            Double amount = ((Number) row[1]).doubleValue();
            categoryBreakdown.put(category, amount);
            System.out.println("📊 Category: " + category + ", Amount: " + amount);
        }
        report.put("categoryBreakdown", categoryBreakdown);
        
        // Daily usage (last 7 days)
        System.out.println("📈 Getting daily usage...");
        Map<String, Long> dailyUsage = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = expenseRepository.countExpensesByDate(date);
            dailyUsage.put(date.toString(), count);
            System.out.println("📈 Date: " + date + ", Count: " + count);
        }
        report.put("dailyUsage", dailyUsage);
        
        System.out.println("✅ System report generated: " + report);
        return report;
    }

    // --- Notifications ---
    public void sendGlobalNotification(String message) {
        System.out.println("🔔 AdminService: Sending global notification: " + message);
        try {
            notificationService.sendGlobalNotification(message);
            System.out.println("✅ AdminService: Global notification sent successfully");
        } catch (Exception e) {
            System.out.println("❌ AdminService: Error sending global notification: " + e.getMessage());
            throw e;
        }
    }
}
