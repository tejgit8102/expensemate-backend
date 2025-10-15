package com.expensemate.expensemate_backend.config;

import com.expensemate.expensemate_backend.model.Role;
import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // ✅ Check if admin exists
            if (userRepository.findByEmail("admin@expensemate.com").isEmpty()) {
                User admin = new User();
                admin.setUsername("Admin");
                admin.setEmail("admin@expensemate.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ROLE_ADMIN); // ✅ using enum Role
                // Skip admin.setActive(true); since your User model doesn’t have it

                userRepository.save(admin);
                System.out.println("✅ Default admin created: admin@expensemate.com / admin123");
            } else {
                System.out.println("ℹ️ Admin already exists. Skipping creation.");
            }
        };
    }
}
