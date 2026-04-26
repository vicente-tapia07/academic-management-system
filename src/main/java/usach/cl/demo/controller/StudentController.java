package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.Student;
import usach.cl.demo.service.StudentService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    // spring inyecta el servicio automaticamente
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // CRUD

    // retorna todos los estudiantes
    @GetMapping
    public ResponseEntity<List<Student>> getAll() {
        List<Student> students = studentService.findAll();
        return ResponseEntity.ok(students);
    }

    // retorna un estudiante por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(@PathVariable Long id) {
        Optional<Student> student = studentService.findById(id);
        return student.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // crea un nuevo estudiante
    @PostMapping
    public ResponseEntity<String> create(@RequestBody Student student) {
        int result = studentService.save(student);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Student created successfully");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating student");
    }

    // actualiza un estudiante existente
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        int result = studentService.update(student);
        if (result > 0) {
            return ResponseEntity.ok("Student updated successfully");
        }
        return ResponseEntity.notFound().build();
    }

    // elimina un estudiante por su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        int result = studentService.deleteById(id);
        if (result > 0) {
            return ResponseEntity.ok("Student deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
}