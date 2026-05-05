package usach.cl.demo.entity;

import usach.cl.demo.model.Role;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class User implements UserDetails {
    private final int id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;

    public User(int id, @Nonnull String name, @Nonnull String email,
                @Nonnull String password, @Nonnull Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    @Override
    @Nonnull
    public String getUsername() {
        return email;
    }

    @Override
    @Nonnull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.toString().toLowerCase());
        return claims;
    }
}