package com.expensemate.expensemate_backend.repository;

import com.expensemate.expensemate_backend.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Get all expenses (no pagination)
    List<Expense> findByUserId(Long userId);

    // Get expenses by user (paged)
    Page<Expense> findByUserId(Long userId, Pageable pageable);

    // Filter by category + user (paged)
    Page<Expense> findByUserIdAndCategory(Long userId, String category, Pageable pageable);

    // Filter by date (month/year) + user (paged)
    Page<Expense> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end, Pageable pageable);

    // For reports: all expenses between dates (no pagination)
    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Expense> findTop10ByOrderByDateDesc();

    // Find flagged expenses for admin audit
    List<Expense> findByFlaggedTrue();


    // Sum of expenses for a month
    @Query("SELECT COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND MONTH(e.date) = :month " +
           "AND YEAR(e.date) = :year")
    double sumExpensesForUserAndMonth(@Param("userId") Long userId,
                                      @Param("month") int month,
                                      @Param("year") int year);

    // Sum of expenses grouped by category for a month
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND MONTH(e.date) = :month " +
           "AND YEAR(e.date) = :year " +
           "GROUP BY e.category")
    List<Object[]> sumExpensesByCategory(@Param("userId") Long userId,
                                         @Param("month") int month,
                                         @Param("year") int year);

    // Find expenses by user, category, month, and year
    @Query("SELECT e FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND e.category = :category " +
           "AND MONTH(e.date) = :month " +
           "AND YEAR(e.date) = :year")
    List<Expense> findByUserIdAndCategoryAndMonth(@Param("userId") Long userId,
                                                  @Param("category") String category,
                                                  @Param("month") int month,
                                                  @Param("year") int year);

    // Get category breakdown for admin reports
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e " +
           "GROUP BY e.category " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategoryBreakdown();

    // Count expenses by date for daily usage report
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.date = :date")
    long countExpensesByDate(@Param("date") LocalDate date);
}