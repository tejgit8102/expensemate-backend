package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.dto.BudgetDto;
import com.expensemate.expensemate_backend.model.Budget;
import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.BudgetRepository;
import com.expensemate.expensemate_backend.repository.ExpenseRepository;
import com.expensemate.expensemate_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    public BudgetService(BudgetRepository budgetRepository,
                         UserRepository userRepository,
                         ExpenseRepository expenseRepository,
                         NotificationService notificationService) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.notificationService = notificationService;
    }

    // ----------------- Set or Create Budget -----------------
    public BudgetDto setBudget(Long userId, BudgetDto dto) {
        return saveOrUpdateBudget(userId, dto, false);
    }

    // ----------------- Update Existing Budget -----------------
    public BudgetDto updateBudget(Long userId, BudgetDto dto) {
        return saveOrUpdateBudget(userId, dto, true);
    }

    // ----------------- Get Budget Status -----------------
    public BudgetDto getBudgetStatus(Long userId, int month, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Budget> optionalBudget = budgetRepository.findByUserAndMonthAndYear(user, month, year);
        double totalSpent = expenseRepository.sumExpensesForUserAndMonth(userId, month, year);

        BudgetDto dto = new BudgetDto();
        dto.setMonth(month);
        dto.setYear(year);
        dto.setTotalSpent(totalSpent);

        if (optionalBudget.isPresent()) {
            Budget b = optionalBudget.get();
            dto.setAmount(b.getAmount());
            dto.setRemaining(b.getAmount() - totalSpent);
            dto.setPercentageUsed(b.getAmount() > 0 ? (totalSpent / b.getAmount()) * 100 : 0);

            // 🔔 Optional: Send overspending notification
            if (totalSpent > b.getAmount()) {
                sendOverspendNotification(userId, month, year);
            }
        } else {
            dto.setAmount(0.0);
            dto.setRemaining(0.0);
            dto.setPercentageUsed(0.0);
        }

        return dto;
    }

    // ----------------- Helper: Save or Update Budget -----------------
    private BudgetDto saveOrUpdateBudget(Long userId, BudgetDto dto, boolean mustExist) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate now = LocalDate.now();
        int month = dto.getMonth() != null ? dto.getMonth() : now.getMonthValue();
        int year = dto.getYear() != null ? dto.getYear() : now.getYear();

        Optional<Budget> optionalBudget = budgetRepository.findByUserAndMonthAndYear(user, month, year);

        if (mustExist && optionalBudget.isEmpty()) {
            throw new RuntimeException("Budget not found for the specified month/year");
        }

        Budget budget = optionalBudget.orElseGet(() -> Budget.builder()
                .user(user)
                .month(month)
                .year(year)
                .build());

        budget.setAmount(dto.getAmount());
        Budget saved = budgetRepository.save(budget);

        double totalSpent = expenseRepository.sumExpensesForUserAndMonth(userId, month, year);

        // 🔔 Optional: Send overspending notification
        if (dto.getAmount() > 0 && totalSpent > dto.getAmount()) {
            sendOverspendNotification(userId, month, year);
        }

        // ✅ Notify only when user sets or updates a budget
        notificationService.notifyReportReady(userId);

        return mapToDto(saved, totalSpent);
    }

    // ----------------- Map Budget entity to DTO -----------------
    private BudgetDto mapToDto(Budget budget, double totalSpent) {
        BudgetDto dto = new BudgetDto();
        dto.setAmount(budget.getAmount());
        dto.setMonth(budget.getMonth());
        dto.setYear(budget.getYear());
        dto.setTotalSpent(totalSpent);
        dto.setRemaining(budget.getAmount() - totalSpent);
        dto.setPercentageUsed(budget.getAmount() > 0 ? (totalSpent / budget.getAmount()) * 100 : 0);
        return dto;
    }

    // ----------------- Send overspending notification -----------------
    private void sendOverspendNotification(Long userId, int month, int year) {
        String monthName = LocalDate.of(year, month, 1)
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        notificationService.createNotification(
                userId,
                "⚠️ You’ve exceeded your budget for " + monthName + "!"
        );
    }
}
