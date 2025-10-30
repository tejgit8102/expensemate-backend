package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.dto.BudgetDto;
import com.expensemate.expensemate_backend.security.JwtUtil;
import com.expensemate.expensemate_backend.service.BudgetService;
import com.expensemate.expensemate_backend.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetService budgetService;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    public BudgetController(BudgetService budgetService,
                            JwtUtil jwtUtil,
                            NotificationService notificationService) {
        this.budgetService = budgetService;
        this.jwtUtil = jwtUtil;
        this.notificationService = notificationService;
    }

    // ----------------- Set or Create Budget -----------------
    @PostMapping
    public ResponseEntity<?> setBudget(@RequestBody BudgetDto dto, HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing JWT");
        }

        BudgetDto saved = budgetService.setBudget(userId, dto);

        return ResponseEntity.ok(saved);
    }

    // ----------------- Update Budget -----------------
    @PutMapping
    public ResponseEntity<?> updateBudget(@RequestBody BudgetDto dto, HttpServletRequest request) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing JWT");
        }

        BudgetDto updated = budgetService.updateBudget(userId, dto);
        return ResponseEntity.ok(updated);
    }

    // ----------------- Get Budget Status -----------------
    @GetMapping
    public ResponseEntity<?> getBudgetStatus(HttpServletRequest request,
                                             @RequestParam(required = false) Integer month,
                                             @RequestParam(required = false) Integer year) {
        Long userId = jwtUtil.extractUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing JWT");
        }

        LocalDate now = LocalDate.now();
        int queryMonth = (month != null) ? month : now.getMonthValue();
        int queryYear = (year != null) ? year : now.getYear();

        BudgetDto status = budgetService.getBudgetStatus(userId, queryMonth, queryYear);
        return ResponseEntity.ok(status);
    }

    // ----------------- Helper: Format month number → Name -----------------
    private String monthName(int month) {
        return LocalDate.of(2000, month, 1)
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
