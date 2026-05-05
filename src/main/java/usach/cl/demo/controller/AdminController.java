package usach.cl.demo.controller;

import usach.cl.demo.entity.User;
import usach.cl.demo.model.UserDto;
import usach.cl.demo.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(adminService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable int id) {
        return ResponseEntity.ok(adminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody UserDto dto) throws Exception{
        return ResponseEntity.ok(adminService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable int id, @RequestBody UserDto dto) {
        adminService.update(id, dto);
        return ResponseEntity.ok(adminService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        adminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}