package com.expensemate.expensemate_backend.repository;

import com.expensemate.expensemate_backend.model.Budget;
import com.expensemate.expensemate_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Find budget by user, month, and year
    Optional<Budget> findByUserAndMonthAndYear(User user, Integer month, Integer year);

    @Query("SELECT AVG(b.amount) FROM Budget b")
    Double getAverageBudgetAmount();

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.spent > b.amount")
    Long countBudgetsOverLimit();

    // Helper method: find by userId directly (used in service if needed)
    default Optional<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year) {
        return findAll().stream()
                .filter(b -> b.getUser().getId().equals(userId)
                        && b.getMonth().equals(month)
                        && b.getYear().equals(year))
                .findFirst();
    }
}
