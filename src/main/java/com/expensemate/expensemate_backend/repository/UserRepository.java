package com.expensemate.expensemate_backend.repository;

import com.expensemate.expensemate_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByUsernameAndIdNot(String username, Long id);

    Boolean existsByEmail(String email);

    List<User> findTop5ByOrderByCreatedAtDesc();

    // Count active users (users with expenses in the last 30 days)
    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Expense e WHERE e.date >= :since")
    long countActiveUsers(@Param("since") LocalDate since);

}
