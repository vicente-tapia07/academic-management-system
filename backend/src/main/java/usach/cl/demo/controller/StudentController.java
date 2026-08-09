package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.service.StudentService;
import usach.cl.demo.service.AuthorizationService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final AuthorizationService authorizationService;

    public StudentController(StudentService studentService, AuthorizationService authorizationService) {
        this.studentService = studentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<StudentEntity>> getAll() {
        return ResponseEntity.ok(studentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentEntity> getById(@PathVariable Long id) {
        Optional<StudentEntity> student = studentService.findById(id);
        return student.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/curriculum")
    public ResponseEntity<List<SubjectStatusDTO>> getCurriculum(@PathVariable Long id,
                                                                 Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, id);
        List<SubjectStatusDTO> curriculum = studentService.findCurriculum(id);
        if (curriculum.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(curriculum);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody StudentDTO dto) {
        try {
            studentService.saveWithUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Estudiante creado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody StudentEntity studentEntity) {
        studentEntity.setId(id);
        studentService.update(id, studentEntity);
        return ResponseEntity.ok("Student updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.ok("Student deleted successfully");
    }
}
