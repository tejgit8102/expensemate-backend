package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.model.Role;
import com.expensemate.expensemate_backend.model.User;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final User user;

    // ✅ Expose user ID for controllers/services
    public Long getId() {
        return user.getId();
    }

    // ✅ Return authorities based on user role
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = user.getRole() != null ? user.getRole() : Role.ROLE_USER;
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // login with email
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ✅ Use `active` flag to determine if the account is enabled
    @Override
    public boolean isEnabled() {
        return user.isActive(); // returns false if admin deactivated user
    }

    // ✅ Optional helper: Get the user's role name directly
    public String getRoleName() {
        return user.getRole() != null ? user.getRole().name() : Role.ROLE_USER.name();
    }

    // ✅ Optional: Expose the whole User if needed
    public User getUser() {
        return user;
    }
}
