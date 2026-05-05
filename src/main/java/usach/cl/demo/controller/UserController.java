package usach.cl.demo.controller;

import usach.cl.demo.entity.User;
import usach.cl.demo.model.UserDto;
import usach.cl.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserDto userDto) throws Exception{
        return ResponseEntity.ok(userService.create(userDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable int id) {
        return ResponseEntity.ok(userService.getById(id));
    }
}