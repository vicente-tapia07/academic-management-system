package usach.cl.demo.service;

import usach.cl.demo.entity.User;
import usach.cl.demo.model.Role;
import usach.cl.demo.model.UserDto;
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

    public User create(UserDto dto) throws Exception {
        return userService.create(
                new UserDto(dto.name(), dto.email(), dto.password(), Role.ADMIN)
        );
    }

    public List<User> getAll() {
        return userRepository.findAllByRole(Role.ADMIN);
    }

    public User getById(int id) {
        User user = userService.getById(id);
        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }
        return user;
    }

    public void update(int id, UserDto dto) {
        userService.updateUser(id, dto.name(), dto.email());
    }

    public void delete(int id) {
        userService.deleteUser(id);
    }
}