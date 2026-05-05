package usach.cl.demo.model;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class UserEntity implements UserDetails {
    private final int id;
    private final String rut;        // antes: name
    private final String email;
    private final String password;
    private final Role role;

    public UserEntity(int id, @Nonnull String rut, @Nonnull String email,
                      @Nonnull String password, @Nonnull Role role) {
        this.id = id;
        this.rut = rut;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}