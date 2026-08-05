package usach.cl.demo.service;


import usach.cl.demo.model.UserEntity;
import usach.cl.demo.dto.UserDTO;
import usach.cl.demo.repository.UserRepository;
import jakarta.annotation.Nonnull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Nonnull
    public UserEntity create(@Nonnull UserDTO userData) throws Exception {
        if (userData.name() == null || userData.name().isBlank() ||
                userData.email() == null || userData.email().isBlank() ||
                userData.password() == null || userData.password().isBlank() ||
                userData.role() == null) {
            throw new IllegalArgumentException("RUT, email, contraseña y rol son obligatorios");
        }
        try {
            return repository.save(
                    new UserEntity(-1, userData.name(), userData.email(),
                            passwordEncoder.encode(userData.password()), userData.role())
            );
        } catch (DuplicateKeyException e) {
            throw new Exception("Email ya esta en uso");
        }
    }

    public UserEntity getById(int id) {
        return repository.findById(id);
    }

    public UserEntity getByEmail(@Nonnull String email) {
        return repository.findByEmail(email);
    }

    public void updateUser(int id, String rut, String email) {
        if (rut == null || rut.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("RUT y email son obligatorios");
        }
        repository.updateUser(id, rut, email);
    }

    public void deleteUser(int id) {
        repository.deleteById(id);
    }

    @Override
    @Nonnull
    public UserDetails loadUserByUsername(@Nonnull String username) throws UsernameNotFoundException {
        return getByEmail(username);
    }
}
