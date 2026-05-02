package pablog.selextrace.security;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pablog.selextrace.model.auth.SystemRole;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails {

    private final String userId;
    private final String email;
    private final String passwordHash;
    private final SystemRole systemRole;
    private final boolean active;

    public AuthenticatedUser(
            String userId,
            String email,
            String passwordHash,
            SystemRole systemRole,
            boolean active
    ) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.systemRole = systemRole;
        this.active = active;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + systemRole.name()));
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

    @Override
    public boolean isEnabled() {
        return active;
    }
}
