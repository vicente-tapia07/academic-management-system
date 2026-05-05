package usach.cl.demo.controller;

import usach.cl.demo.model.UserEntity;
import usach.cl.demo.dto.UserDTO;
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
    public ResponseEntity<List<UserEntity>> getAll() {
        return ResponseEntity.ok(adminService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getById(@PathVariable int id) {
        return ResponseEntity.ok(adminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UserEntity> create(@RequestBody UserDTO dto) throws Exception{
        return ResponseEntity.ok(adminService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> update(@PathVariable int id, @RequestBody UserDTO dto) {
        adminService.update(id, dto);
        return ResponseEntity.ok(adminService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        adminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}