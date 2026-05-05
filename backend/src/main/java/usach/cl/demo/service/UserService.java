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

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    public UserEntity create(@Nonnull UserDTO userData) throws Exception {
        try {
            return repository.save(
                    new UserEntity(-1, userData.name(), userData.email(),
                            userData.password(), userData.role())
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

    public void updateUser(int id, String name, String email) {
        repository.updateUser(id, name, email);
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