package usach.cl.demo.service;

import usach.cl.demo.model.UserEntity;
import usach.cl.demo.model.Role;
import usach.cl.demo.dto.UserDTO;
import usach.cl.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserService userService;
    private final UserRepository userRepository;

    public AdminService(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public UserEntity create(UserDTO dto) throws Exception {
        return userService.create(
                new UserDTO(dto.name(), dto.email(), dto.password(), Role.ADMIN)
        );
    }

    public List<UserEntity> getAll() {
        return userRepository.findAllByRole(Role.ADMIN);
    }

    public UserEntity getById(int id) {
        UserEntity user = userService.getById(id);
        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }
        return user;
    }

    public void update(int id, UserDTO dto) {
        getById(id);
        userService.updateUser(id, dto.name(), dto.email());
    }

    public void delete(int id) {
        getById(id);
        userService.deleteUser(id);
    }
}
