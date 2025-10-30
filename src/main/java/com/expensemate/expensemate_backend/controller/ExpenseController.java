package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.dto.ExpenseDto;
import com.expensemate.expensemate_backend.model.Expense;
import com.expensemate.expensemate_backend.security.JwtUtil;
import com.expensemate.expensemate_backend.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private JwtUtil jwtUtil;

    // ----------------- Add Expense -----------------
    @PostMapping
    public ResponseEntity<Expense> addExpense(@RequestBody ExpenseDto dto, HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        Expense saved = expenseService.addExpense(userId, dto);
        return ResponseEntity.ok(saved);
    }

    // ----------------- Get Expenses (filters + pagination) -----------------
    @GetMapping
    public ResponseEntity<Page<Expense>> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Long userId = jwtUtil.extractUserIdFromRequest(request);
        Page<Expense> expenses = expenseService.getExpenses(userId, category, date, PageRequest.of(page, size));
        return ResponseEntity.ok(expenses);
    }

    // ----------------- Update Expense -----------------
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id,
            @RequestBody ExpenseDto dto,
            HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        Expense updated = expenseService.updateExpense(id, dto, userId); // pass userId to check ownership
        return ResponseEntity.ok(updated);
    }

    // ----------------- Delete Expense -----------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id, HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        expenseService.deleteExpense(id, userId); // pass userId to check ownership
        return ResponseEntity.noContent().build();
    }

    // ----------------- Total Monthly Spending -----------------
    @GetMapping("/total/{month}/{year}")
    public ResponseEntity<Map<String, Object>> getTotalMonthlySpending(@PathVariable int month,
            @PathVariable int year,
            HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        double totalSpent = expenseService.getTotalMonthlySpending(userId, month, year);
        return ResponseEntity.ok(Map.of(
                "month", month,
                "year", year,
                "totalSpent", totalSpent));
    }

    // ----------------- Spending by Category -----------------
    @GetMapping("/categories/{month}/{year}")
    public ResponseEntity<Map<String, Double>> getSpendingByCategory(@PathVariable int month,
            @PathVariable int year,
            HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        Map<String, Double> spendingByCategory = expenseService.getSpendingByCategory(userId, month, year);
        return ResponseEntity.ok(spendingByCategory);
    }

    // ----------------- Monthly Expense Report -----------------
    @GetMapping("/report/{month}/{year}")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(@PathVariable int month,
            @PathVariable int year,
            HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);

        double totalSpent = expenseService.getTotalMonthlySpending(userId, month, year);
        Map<String, Double> spendingByCategory = expenseService.getSpendingByCategory(userId, month, year);

        Map<String, Object> report = Map.of(
                "month", month,
                "year", year,
                "totalSpent", totalSpent,
                "spendingByCategory", spendingByCategory);

        return ResponseEntity.ok(report);
    }

}
