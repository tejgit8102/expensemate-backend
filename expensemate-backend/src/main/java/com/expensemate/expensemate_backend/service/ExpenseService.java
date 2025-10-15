package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.dto.ExpenseDto;
import com.expensemate.expensemate_backend.model.Budget;
import com.expensemate.expensemate_backend.model.Expense;
import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.BudgetRepository;
import com.expensemate.expensemate_backend.repository.ExpenseRepository;
import com.expensemate.expensemate_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private NotificationService notificationService;

    // ----------------- Add Expense -----------------
    public Expense addExpense(Long userId, ExpenseDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Expense expense = new Expense();
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());
        expense.setDate(dto.getDate());
        expense.setUser(user);

        Expense savedExpense = expenseRepository.save(expense);

        // ✅ Trigger notification for new expense
        notificationService.notifyExpenseAdded(userId, savedExpense.getAmount(), savedExpense.getCategory());

        // ---------------- Budget limit checks ----------------
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonthAndYear(
                userId, savedExpense.getDate().getMonthValue(), savedExpense.getDate().getYear()
        );

        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();

            double totalSpent = getTotalSpentForCategory(userId, savedExpense.getCategory(),
                    savedExpense.getDate().getMonthValue(), savedExpense.getDate().getYear());

            double percentSpent = (totalSpent / budget.getAmount()) * 100;

            if (totalSpent > budget.getAmount()) {
                notificationService.notifyBudgetExceeded(userId, savedExpense.getCategory());
            } else if (percentSpent >= 80) {
                notificationService.notifyBudgetNearingLimit(userId, savedExpense.getCategory(), percentSpent);
            }
        }

        return savedExpense;
    }

    // ----------------- Helper: total spent in month for category -----------------
    private double getTotalSpentForCategory(Long userId, String category, int month, int year) {
        // Custom repository method required:
        // List<Expense> findByUserIdAndCategoryAndMonth(Long userId, String category, int month, int year);
        List<Expense> expenses = expenseRepository.findByUserIdAndCategoryAndMonth(userId, category, month, year);
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    // ----------------- Get Expenses with filters -----------------
    public Page<Expense> getExpenses(Long userId, String category, String date, Pageable pageable) {
        if (category != null && !category.isEmpty()) {
            return expenseRepository.findByUserIdAndCategory(userId, category, pageable);
        }

        if (date != null && !date.isEmpty()) { // format: yyyy-MM
            LocalDate start = LocalDate.parse(date + "-01");
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return expenseRepository.findByUserIdAndDateBetween(userId, start, end, pageable);
        }

        return expenseRepository.findByUserId(userId, pageable);
    }

    // ----------------- Update Expense -----------------
    public Expense updateExpense(Long id, ExpenseDto dto, Long userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Cannot update expense of another user");
        }

        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());
        expense.setDate(dto.getDate());

        return expenseRepository.save(expense);
    }

    // ----------------- Delete Expense -----------------
    public void deleteExpense(Long id, Long userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Cannot delete expense of another user");
        }

        expenseRepository.delete(expense);
    }

    // ----------------- Total Monthly Spending -----------------
    public double getTotalMonthlySpending(Long userId, int month, int year) {
        return expenseRepository.sumExpensesForUserAndMonth(userId, month, year);
    }

    // ----------------- Spending by Category -----------------
    public Map<String, Double> getSpendingByCategory(Long userId, int month, int year) {
        List<Object[]> results = expenseRepository.sumExpensesByCategory(userId, month, year);
        Map<String, Double> map = new HashMap<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            Double amount = ((Number) row[1]).doubleValue();
            map.put(category, amount);
        }
        return map;
    }

    // ----------------- Expenses for Report -----------------
    public List<Expense> getExpensesForMonth(Long userId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return expenseRepository.findByUserIdAndDateBetween(userId, start, end);
    }
}
